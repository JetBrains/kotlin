/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.LLVMStoreSizeOfType
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.backend.konan.descriptors.getStringValue
import org.jetbrains.kotlin.backend.konan.descriptors.getStringValueOrNull
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.isFinalClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces

internal class KotlinObjCClassInfoGenerator(override val context: Context) : ContextUtils {
    fun generate(irClass: IrClass) {
        val descriptor = irClass.descriptor
        assert(descriptor.isFinalClass)

        val objCLLvmDeclarations = context.llvmDeclarations.forClass(descriptor).objCDeclarations!!

        val instanceMethods = generateInstanceMethodDescs(irClass)

        val companionObjectDescriptor = descriptor.companionObjectDescriptor
        val classMethods = companionObjectDescriptor?.generateOverridingMethodDescs() ?: emptyList()

        val superclassName = descriptor.getSuperClassNotAny()!!.let {
            context.llvm.imports.add(it.llvmSymbolOrigin)
            it.name.asString()
        }
        val protocolNames = descriptor.getSuperInterfaces().map {
            context.llvm.imports.add(it.llvmSymbolOrigin)
            it.name.asString().removeSuffix("Protocol")
        }

        val bodySize =
                LLVMStoreSizeOfType(llvmTargetData, context.llvmDeclarations.forClass(descriptor).bodyType).toInt()

        val className = selectClassName(descriptor)?.let { staticData.cStringLiteral(it) } ?: NullPointer(int8Type)

        val info = Struct(runtime.kotlinObjCClassInfo,
                className,

                staticData.cStringLiteral(superclassName),
                staticData.placeGlobalConstArray("", int8TypePtr,
                        protocolNames.map { staticData.cStringLiteral(it) } + NullPointer(int8Type)),

                staticData.placeGlobalConstArray("", runtime.objCMethodDescription, instanceMethods),
                Int32(instanceMethods.size),

                staticData.placeGlobalConstArray("", runtime.objCMethodDescription, classMethods),
                Int32(classMethods.size),

                Int32(bodySize),
                objCLLvmDeclarations.bodyOffsetGlobal.pointer,

                descriptor.typeInfoPtr,
                companionObjectDescriptor?.typeInfoPtr ?: NullPointer(runtime.typeInfoType),

                objCLLvmDeclarations.classPointerGlobal.pointer
        )

        objCLLvmDeclarations.classInfoGlobal.setInitializer(info)

        objCLLvmDeclarations.classPointerGlobal.setInitializer(NullPointer(int8Type))
        objCLLvmDeclarations.bodyOffsetGlobal.setInitializer(Int32(0))
    }

    private fun generateInstanceMethodDescs(
            irClass: IrClass
    ): List<ObjCMethodDesc> = mutableListOf<ObjCMethodDesc>().apply {
        val descriptor = irClass.descriptor
        addAll(descriptor.generateOverridingMethodDescs())
        addAll(irClass.generateImpMethodDescs())
        val allImplementedSelectors = this.map { it.selector }.toSet()

        assert(descriptor.getSuperClassNotAny()!!.isExternalObjCClass())
        val allInitMethodsInfo = descriptor.getSuperClassNotAny()!!.constructors
                .mapNotNull { it.getObjCInitMethod()?.getExternalObjCMethodInfo() }
                .filter { it.selector !in allImplementedSelectors }
                .distinctBy { it.selector }

        allInitMethodsInfo.mapTo(this) {
            ObjCMethodDesc(it.selector, it.encoding, context.llvm.missingInitImp)
        }
    }

    private fun selectClassName(descriptor: ClassDescriptor): String? {
        val exportObjCClassAnnotation =
                descriptor.annotations.findAnnotation(context.interopBuiltIns.exportObjCClass.fqNameSafe)

        return if (exportObjCClassAnnotation != null) {
            exportObjCClassAnnotation.getStringValueOrNull("name") ?: descriptor.name.asString()
        } else if (descriptor.isExported()) {
            descriptor.fqNameSafe.asString()
        } else {
            null // Generate as anonymous.
        }
    }

    private val impType = pointerType(functionType(int8TypePtr, true, int8TypePtr, int8TypePtr))

    private inner class ObjCMethodDesc(
            val selector: String, val encoding: String, val impFunction: LLVMValueRef
    ) : Struct(
            runtime.objCMethodDescription,
            constPointer(impFunction).bitcast(impType),
            staticData.cStringLiteral(selector),
            staticData.cStringLiteral(encoding)
    )

    private fun generateMethodDesc(info: ObjCMethodInfo) = ObjCMethodDesc(
            info.selector,
            info.encoding,
            context.llvm.externalFunction(
                    info.imp,
                    functionType(voidType),
                    origin = info.bridge.llvmSymbolOrigin
            )
    )

    private fun ClassDescriptor.generateOverridingMethodDescs(): List<ObjCMethodDesc> =
            this.unsubstitutedMemberScope.contributedMethods.filter {
                it.kind.isReal && it !is ConstructorDescriptor
            }.mapNotNull { it.getObjCMethodInfo() }.map { generateMethodDesc(it) }

    private fun IrClass.generateImpMethodDescs(): List<ObjCMethodDesc> = this.declarations
            .filterIsInstance<IrSimpleFunction>()
            .mapNotNull {
                val annotation =
                        it.descriptor.annotations.findAnnotation(context.interopBuiltIns.objCMethodImp.fqNameSafe) ?:
                                return@mapNotNull null

                ObjCMethodDesc(
                        annotation.getStringValue("selector"),
                        annotation.getStringValue("encoding"),
                        it.descriptor.llvmFunction
                )
            }
}