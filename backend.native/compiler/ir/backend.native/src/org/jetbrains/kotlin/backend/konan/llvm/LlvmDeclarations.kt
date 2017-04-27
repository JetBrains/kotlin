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

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

internal fun createLlvmDeclarations(context: Context): LlvmDeclarations {
    val generator = DeclarationsGeneratorVisitor(context)
    context.ir.irModule.acceptChildrenVoid(generator)
    return with(generator) {
        LlvmDeclarations(
                functions, classes, fields, staticFields, theUnitInstanceRef
        )
    }

}

internal class LlvmDeclarations(
        private val functions: Map<FunctionDescriptor, FunctionLlvmDeclarations>,
        private val classes: Map<ClassDescriptor, ClassLlvmDeclarations>,
        private val fields: Map<PropertyDescriptor, FieldLlvmDeclarations>,
        private val staticFields: Map<PropertyDescriptor, StaticFieldLlvmDeclarations>,
        private val theUnitInstanceRef: ConstPointer?
) {
    fun forFunction(descriptor: FunctionDescriptor) = functions[descriptor] ?:
            error(descriptor.toString())

    fun forClass(descriptor: ClassDescriptor) = classes[descriptor] ?:
            error(descriptor.toString())

    fun forField(descriptor: PropertyDescriptor) = fields[descriptor] ?:
            error(descriptor.toString())

    fun forStaticField(descriptor: PropertyDescriptor) = staticFields[descriptor] ?:
            error(descriptor.toString())

    fun forSingleton(descriptor: ClassDescriptor) = forClass(descriptor).singletonDeclarations ?:
            error(descriptor.toString())

    fun getUnitInstanceRef() = theUnitInstanceRef ?: error("")

}

internal class ClassLlvmDeclarations(
        val bodyType: LLVMTypeRef,
        val fields: List<PropertyDescriptor>, // TODO: it is not an LLVM declaration.
        val typeInfoGlobal: StaticData.Global,
        val typeInfo: ConstPointer,
        val singletonDeclarations: SingletonLlvmDeclarations?)

internal class SingletonLlvmDeclarations(val instanceFieldRef: LLVMValueRef)

internal class FunctionLlvmDeclarations(val llvmFunction: LLVMValueRef)

internal class FieldLlvmDeclarations(val index: Int, val classBodyType: LLVMTypeRef)

internal class StaticFieldLlvmDeclarations(val storage: LLVMValueRef)

// TODO: rework getFields and getDeclaredFields.

/**
 * All fields of the class instance.
 * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
 */
internal fun ContextUtils.getFields(classDescriptor: ClassDescriptor): List<PropertyDescriptor> {
    val superClass = classDescriptor.getSuperClassNotAny() // TODO: what if Any has fields?
    val superFields = if (superClass != null) getFields(superClass) else emptyList()

    return superFields + getDeclaredFields(classDescriptor)
}

/**
 * Fields declared in the class.
 */
private fun ContextUtils.getDeclaredFields(classDescriptor: ClassDescriptor): List<PropertyDescriptor> {
    // TODO: Here's what is going on here:
    // The existence of a backing field for a property is only described in the IR,
    // but not in the PropertyDescriptor.
    //
    // We mark serialized properties with a Konan protobuf extension bit,
    // so it is present in DeserializedPropertyDescriptor.
    //
    // In this function we check the presence of the backing filed
    // two ways: first we check IR, then we check the protobuf extension.

    val irClass = context.ir.moduleIndexForCodegen.classes[classDescriptor]
    val fields = if (irClass != null) {
        val declarations = irClass.declarations

        declarations.mapNotNull {
            when (it) {
                is IrProperty -> it.backingField?.descriptor
                is IrField -> it.descriptor
                else -> null
            }
        }
    } else {
        val properties = classDescriptor.unsubstitutedMemberScope.
                getContributedDescriptors().
                filterIsInstance<DeserializedPropertyDescriptor>()

        properties.mapNotNull { it.backingField }
    }
    return fields.sortedBy {
        it.fqNameSafe.localHash.value
    }
}

private fun ContextUtils.createClassBodyType(name: String, fields: List<PropertyDescriptor>): LLVMTypeRef {
    val fieldTypes = fields.map { getLLVMType(if (it.isDelegated) context.builtIns.nullableAnyType else it.type) }

    val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), name)!!

    LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, 0)

    return classType
}

private class DeclarationsGeneratorVisitor(override val context: Context) :
        IrElementVisitorVoid, ContextUtils {

    val functions = mutableMapOf<FunctionDescriptor, FunctionLlvmDeclarations>()
    val classes = mutableMapOf<ClassDescriptor, ClassLlvmDeclarations>()
    val fields = mutableMapOf<PropertyDescriptor, FieldLlvmDeclarations>()
    val staticFields = mutableMapOf<PropertyDescriptor, StaticFieldLlvmDeclarations>()
    var theUnitInstanceRef: ConstPointer? = null

    private class Namer(val prefix: String) {
        private val names = mutableMapOf<DeclarationDescriptor, Name>()
        private val counts = mutableMapOf<FqName, Int>()

        fun getName(parent: FqName, descriptor: DeclarationDescriptor): Name {
            return names.getOrPut(descriptor) {
                val count = counts.getOrDefault(parent, 0) + 1
                counts[parent] = count
                Name.identifier(prefix + count)
            }
        }
    }

    val objectNamer = Namer("object-")

    private fun getLocalName(parent: FqName, descriptor: DeclarationDescriptor): Name {
        if (DescriptorUtils.isAnonymousObject(descriptor)) {
            return objectNamer.getName(parent, descriptor)
        }

        return descriptor.name
    }

    private fun getFqName(descriptor: DeclarationDescriptor): FqName {
        if (descriptor is PackageFragmentDescriptor) {
            return descriptor.fqName
        }


        val containingDeclaration = descriptor.containingDeclaration
        val parent = if (containingDeclaration != null) {
            getFqName(containingDeclaration)
        } else {
            FqName.ROOT
        }

        val localName = getLocalName(parent, descriptor)
        return parent.child(localName)
    }

    /**
     * Produces the name to be used for non-exported LLVM declarations corresponding to [descriptor].
     *
     * Note: since these declarations are going to be private, the name is only required not to clash with any
     * exported declarations.
     */
    private fun qualifyInternalName(descriptor: DeclarationDescriptor): String {
        return getFqName(descriptor).asString() + "#internal"
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {

        if (declaration.descriptor.isIntrinsic) {
            // do not generate any declarations for intrinsic classes as they require special handling
        } else {
            this.classes[declaration.descriptor] = createClassDeclarations(declaration)
        }

        super.visitClass(declaration)
    }

    private fun createClassDeclarations(declaration: IrClass): ClassLlvmDeclarations {
        val descriptor = declaration.descriptor

        val internalName = qualifyInternalName(descriptor)

        val fields = getFields(descriptor)
        val bodyType = createClassBodyType("kclassbody:$internalName", fields)

        val typeInfoPtr: ConstPointer
        val typeInfoGlobal: StaticData.Global

        val typeInfoSymbolName = if (descriptor.isExported()) {
            descriptor.typeInfoSymbolName
        } else {
            "ktype:$internalName"
        }

        if (!descriptor.isAbstract()) {
            // Create the special global consisting of TypeInfo and vtable.

            val typeInfoGlobalName = "ktypeglobal:$internalName"

            val typeInfoWithVtableType = structType(
                    runtime.typeInfoType,
                    LLVMArrayType(int8TypePtr, context.getVtableBuilder(descriptor).vtableEntries.size)!!
            )

            typeInfoGlobal = staticData.createGlobal(typeInfoWithVtableType, typeInfoGlobalName, isExported = false)

            val llvmTypeInfoPtr = LLVMAddAlias(context.llvmModule,
                    kTypeInfoPtr,
                    typeInfoGlobal.pointer.getElementPtr(0).llvm,
                    typeInfoSymbolName)!!

            if (!descriptor.isExported()) {
                LLVMSetLinkage(llvmTypeInfoPtr, LLVMLinkage.LLVMInternalLinkage)
            }

            typeInfoPtr = constPointer(llvmTypeInfoPtr)

        } else {
            typeInfoGlobal = staticData.createGlobal(runtime.typeInfoType,
                    typeInfoSymbolName,
                    isExported = descriptor.isExported())

            typeInfoPtr = typeInfoGlobal.pointer
        }

        val singletonDeclarations = if (descriptor.kind.isSingleton) {
            createSingletonDeclarations(descriptor, typeInfoPtr, bodyType)
        } else {
            null
        }

        return ClassLlvmDeclarations(bodyType, fields, typeInfoGlobal, typeInfoPtr, singletonDeclarations)
    }

    private fun createSingletonDeclarations(
            descriptor: ClassDescriptor,
            typeInfoPtr: ConstPointer,
            bodyType: LLVMTypeRef
    ): SingletonLlvmDeclarations? {

        if (descriptor.isUnit()) {
            this.theUnitInstanceRef = staticData.createUnitInstance(descriptor, bodyType, typeInfoPtr)
            return null
        }

        val symbolName = if (descriptor.isExported()) {
            descriptor.objectInstanceFieldSymbolName
        } else {
            "kobjref:" + qualifyInternalName(descriptor)
        }
        val instanceFieldRef = addGlobal(
                symbolName, getLLVMType(descriptor.defaultType), threadLocal = true)

        return SingletonLlvmDeclarations(instanceFieldRef)
    }

    override fun visitField(declaration: IrField) {
        super.visitField(declaration)

        val descriptor = declaration.descriptor

        val dispatchReceiverParameter = descriptor.dispatchReceiverParameter
        if (dispatchReceiverParameter != null) {
            val containingClass = dispatchReceiverParameter.containingDeclaration
            val classDeclarations = this.classes[containingClass] ?: error(containingClass.toString())

            val allFields = classDeclarations.fields

            this.fields[descriptor] = FieldLlvmDeclarations(
                    allFields.indexOf(descriptor),
                    classDeclarations.bodyType
            )
        } else {

            // Fields are module-private, so we use internal name:
            val name = "kvar:" + qualifyInternalName(descriptor)

            val storage = addGlobal(
                    name, getLLVMType(descriptor.type), threadLocal = true)

            this.staticFields[descriptor] = StaticFieldLlvmDeclarations(storage)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        super.visitFunction(declaration)

        val descriptor = declaration.descriptor
        val llvmFunctionType = getLlvmFunctionType(descriptor)

        val llvmFunction = if (descriptor.isExternal) {
            if (descriptor.isIntrinsic) {
                return
            }

            context.llvm.externalFunction(descriptor.symbolName, llvmFunctionType)
        } else {
            val symbolName = if (descriptor.isExported()) {
                descriptor.symbolName
            } else {
                "kfun:" + qualifyInternalName(descriptor)
            }
            LLVMAddFunction(context.llvmModule, symbolName, llvmFunctionType)!!
        }

        if (!context.config.configuration.getBoolean(KonanConfigKeys.OPTIMIZATION)) {
            LLVMAddTargetDependentFunctionAttr(llvmFunction, "no-frame-pointer-elim", "true")
        }

        this.functions[descriptor] = FunctionLlvmDeclarations(llvmFunction)
    }
}
