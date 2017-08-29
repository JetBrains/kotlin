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
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ObjCMethodInfo
import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.backend.konan.descriptors.getStringValue
import org.jetbrains.kotlin.backend.konan.descriptors.getStringValueOrNull
import org.jetbrains.kotlin.backend.konan.getObjCMethodInfo
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

        val instanceMethods = descriptor.generateOverridingMethodDescs() + irClass.generateImpMethodDescs()

        val companionObjectDescriptor = descriptor.companionObjectDescriptor
        val classMethods = companionObjectDescriptor?.generateOverridingMethodDescs() ?: emptyList()

        val superclassName = descriptor.getSuperClassNotAny()!!.name.asString()
        val protocolNames = descriptor.getSuperInterfaces().map { it.name.asString().removeSuffix("Protocol") }

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

    private fun createMethodDesc(selector: String, encoding: String, imp: ConstPointer) = Struct(
            runtime.objCMethodDescription,
            staticData.cStringLiteral(selector),
            staticData.cStringLiteral(encoding),
            imp
    )

    private fun generateMethodDesc(info: ObjCMethodInfo): ConstValue {
        return createMethodDesc(
                info.selector,
                info.encoding,
                constPointer(context.llvm.externalFunction(info.imp, functionType(voidType))).bitcast(int8TypePtr)
        )
    }

    private fun ClassDescriptor.generateOverridingMethodDescs(): List<ConstValue> =
            this.unsubstitutedMemberScope.contributedMethods.filter {
                it.kind.isReal && it !is ConstructorDescriptor
            }.mapNotNull { it.getObjCMethodInfo() }.map { generateMethodDesc(it) }

    private fun IrClass.generateImpMethodDescs(): List<ConstValue> = this.declarations
            .filterIsInstance<IrSimpleFunction>()
            .mapNotNull {
                val annotation =
                        it.descriptor.annotations.findAnnotation(context.interopBuiltIns.objCMethodImp.fqNameSafe) ?:
                                return@mapNotNull null

                createMethodDesc(
                        annotation.getStringValue("selector"),
                        annotation.getStringValue("encoding"),
                        it.descriptor.entryPointAddress
                )
            }
}