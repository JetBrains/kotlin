/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.IrElementVisitorVoidWithContext
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes

internal fun KotlinType?.createExtensionReceiver(owner: CallableDescriptor): ReceiverParameterDescriptor? =
    DescriptorFactory.createExtensionReceiverParameterForCallable(
        owner,
        this,
        Annotations.EMPTY
    )

fun ReferenceSymbolTable.translateErased(type: KotlinType): IrSimpleType {
    val descriptor = TypeUtils.getClassDescriptor(type) ?: return translateErased(type.immediateSupertypes().first())
    val classSymbol = this.referenceClass(descriptor)

    val nullable = type.isMarkedNullable
    val arguments = type.arguments.map { IrStarProjectionImpl }

    return classSymbol.createType(nullable, arguments)
}

internal class DeepCopyIrTreeWithSymbolsForInliner(val context: Context,
                                                   val typeArguments: Map<IrTypeParameterSymbol, IrType?>?,
                                                   val parent: IrDeclarationParent?) : IrCopierForInliner {

    override fun copy(irElement: IrElement): IrElement {
        // Create new symbols.
        irElement.acceptVoid(symbolRemapper)

        // Make symbol remapper aware of the callsite's type arguments.
        symbolRemapper.typeArguments = typeArguments

        // Copy IR.
        val result = irElement.transform(copier, data = null)

        // Bind newly created IR with wrapped descriptors.
        result.acceptVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                (declaration.descriptor as WrappedClassDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                (declaration.descriptor as WrappedClassConstructorDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitEnumEntry(declaration: IrEnumEntry) {
                (declaration.descriptor as WrappedClassDescriptor).bind(
                        declaration.correspondingClass ?: declaration.parentAsClass)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitField(declaration: IrField) {
                (declaration.descriptor as WrappedFieldDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                (declaration.descriptor as WrappedSimpleFunctionDescriptor).bind(declaration as IrSimpleFunction)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
                (declaration.descriptor as? WrappedValueParameterDescriptor)?.bind(declaration)
                (declaration.descriptor as? WrappedReceiverParameterDescriptor)?.bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitTypeParameter(declaration: IrTypeParameter) {
                (declaration.descriptor as WrappedTypeParameterDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }

            override fun visitVariable(declaration: IrVariable) {
                (declaration.descriptor as WrappedVariableDescriptor).bind(declaration)
                declaration.acceptChildrenVoid(this)
            }
        })

        result.patchDeclarationParents(parent)
        return result
    }

    private var nameIndex = 0

    private fun generateCopyName(name: Name) = Name.identifier(name.toString() + "_" + (nameIndex++).toString())

    private inner class InlinerSymbolRenamer : SymbolRenamer {
        private val map = mutableMapOf<IrSymbol, Name>()

        override fun getClassName(symbol: IrClassSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getFunctionName(symbol: IrSimpleFunctionSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getFieldName(symbol: IrFieldSymbol) = symbol.owner.name
        override fun getFileName(symbol: IrFileSymbol) = symbol.owner.fqName
        override fun getExternalPackageFragmentName(symbol: IrExternalPackageFragmentSymbol) = symbol.owner.fqName
        override fun getEnumEntryName(symbol: IrEnumEntrySymbol) = symbol.owner.name
        override fun getVariableName(symbol: IrVariableSymbol) = map.getOrPut(symbol) { generateCopyName(symbol.owner.name) }
        override fun getTypeParameterName(symbol: IrTypeParameterSymbol) = symbol.owner.name
        override fun getValueParameterName(symbol: IrValueParameterSymbol) = symbol.owner.name
    }

    private inner class DescriptorsToIrRemapper : DescriptorsRemapper {
        override fun remapDeclaredClass(descriptor: ClassDescriptor) =
                WrappedClassDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredConstructor(descriptor: ClassConstructorDescriptor) =
                WrappedClassConstructorDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredEnumEntry(descriptor: ClassDescriptor) =
                WrappedClassDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredField(descriptor: PropertyDescriptor) =
                WrappedFieldDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor) =
                WrappedSimpleFunctionDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredTypeParameter(descriptor: TypeParameterDescriptor) =
                WrappedTypeParameterDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredVariable(descriptor: VariableDescriptor) =
                WrappedVariableDescriptor(descriptor.annotations, descriptor.source)

        override fun remapDeclaredValueParameter(descriptor: ParameterDescriptor): ParameterDescriptor =
                if (descriptor is ReceiverParameterDescriptor)
                    WrappedReceiverParameterDescriptor(descriptor.annotations, descriptor.source)
                else
                    WrappedValueParameterDescriptor(descriptor.annotations, descriptor.source)
    }

    private inner class InlinerTypeRemapper(val symbolRemapper: SymbolRemapper,
                                            val typeArguments: Map<IrTypeParameterSymbol, IrType?>?) : TypeRemapper {

        override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) { }

        override fun leaveScope() { }

        private fun remapTypeArguments(arguments: List<IrTypeArgument>) =
                arguments.map { argument ->
                    (argument as? IrTypeProjection)?.let { makeTypeProjection(remapType(it.type), it.variance) }
                            ?: argument
                }

        override fun remapType(type: IrType): IrType {
            if (type !is IrSimpleType) return type

            val substitutedType = typeArguments?.get(type.classifier)
            if (substitutedType != null) {
                substitutedType as IrSimpleType
                return IrSimpleTypeImpl(
                        kotlinType      = null,
                        classifier      = substitutedType.classifier,
                        hasQuestionMark = type.hasQuestionMark or substitutedType.isMarkedNullable(),
                        arguments       = substitutedType.arguments,
                        annotations     = substitutedType.annotations
                )
            }

            return IrSimpleTypeImpl(
                    kotlinType      = null,
                    classifier      = symbolRemapper.getReferencedClassifier(type.classifier),
                    hasQuestionMark = type.hasQuestionMark,
                    arguments       = remapTypeArguments(type.arguments),
                    annotations     = type.annotations.map { it.transform(copier, null) as IrCall }
            )
        }
    }

    override fun addCurrentSubstituteMap(globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>) { }

    private class SymbolRemapperImpl(descriptorsRemapper: DescriptorsRemapper)
        : DeepCopySymbolRemapper(descriptorsRemapper) {

        var typeArguments: Map<IrTypeParameterSymbol, IrType?>? = null
            set(value) {
                if (field != null) return
                field = value?.asSequence()?.associate {
                    (getReferencedClassifier(it.key) as IrTypeParameterSymbol) to it.value
                }
            }

        override fun getReferencedClassifier(symbol: IrClassifierSymbol): IrClassifierSymbol {
            val result = super.getReferencedClassifier(symbol)
            if (result !is IrTypeParameterSymbol)
                return result
            return typeArguments?.get(result)?.classifierOrNull ?: result
        }
    }

    private val symbolRemapper = SymbolRemapperImpl(DescriptorsToIrRemapper())
    private val copier = DeepCopyIrTreeWithSymbols(
            symbolRemapper,
            InlinerTypeRemapper(symbolRemapper, typeArguments),
            InlinerSymbolRenamer()
    )
}

internal interface IrCopierForInliner {
    fun copy(irElement: IrElement): IrElement
    fun addCurrentSubstituteMap(globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>)
}

internal class DeepCopyIrTreeWithDescriptors(val targetDescriptor: FunctionDescriptor,
                                             val parentDescriptor: DeclarationDescriptor,
                                             val context: JsIrBackendContext,
                                             val typeSubstitutor: TypeSubstitutor?) : IrCopierForInliner {

    private val descriptorSubstituteMap: MutableMap<DeclarationDescriptor, DeclarationDescriptor> = mutableMapOf()
    private var nameIndex = 0

    //-------------------------------------------------------------------------//

    override fun copy(irElement: IrElement): IrElement {
        // Create all class descriptors and all necessary descriptors in order to create KotlinTypes.
        irElement.acceptVoid(DescriptorCollectorCreatePhase())
        // Initialize all created descriptors possibly using previously created types.
        irElement.acceptVoid(DescriptorCollectorInitPhase())
        return irElement.accept(InlineCopyIr(), null)
    }

    inner class DescriptorCollectorCreatePhase : IrElementVisitorVoidWithContext() {

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitClassNew(declaration: IrClass) {
            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyClassDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            descriptorSubstituteMap[oldDescriptor.thisAsReceiverParameter] = newDescriptor.thisAsReceiverParameter

            super.visitClassNew(declaration)

            val constructors = oldDescriptor.constructors.map { oldConstructorDescriptor ->
                descriptorSubstituteMap[oldConstructorDescriptor] as ClassConstructorDescriptor
            }.toSet()

            val oldPrimaryConstructor = oldDescriptor.unsubstitutedPrimaryConstructor
            val primaryConstructor = oldPrimaryConstructor?.let { descriptorSubstituteMap[it] as ClassConstructorDescriptor }

            val contributedDescriptors = oldDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map { descriptorSubstituteMap[it]!! }
            newDescriptor.initialize(
                    SimpleMemberScope(contributedDescriptors),
                    constructors,
                    primaryConstructor
            )
        }

        //---------------------------------------------------------------------//

        override fun visitPropertyNew(declaration: IrProperty) {
            copyPropertyOrField(declaration.descriptor)
            super.visitPropertyNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFieldNew(declaration: IrField) {
            val oldDescriptor = declaration.descriptor
            if (descriptorSubstituteMap[oldDescriptor] == null) {
                copyPropertyOrField(oldDescriptor)                                          // A field without a property or a field of a delegated property.
            }
            super.visitFieldNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunctionNew(declaration: IrFunction) {
            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) {                             // Property accessors are copied along with their property.
                val oldContainingDeclaration =
                        if (oldDescriptor.visibility == Visibilities.LOCAL)
                            parentDescriptor
                        else
                            oldDescriptor.containingDeclaration
                descriptorSubstituteMap[oldDescriptor] = copyFunctionDescriptor(oldDescriptor, oldContainingDeclaration)
            }
            super.visitFunctionNew(declaration)
        }

        //--- Copy descriptors ------------------------------------------------//

        private fun generateCopyName(name: Name): Name {
            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(declarationName + "_" + indexStr)
        }

        //---------------------------------------------------------------------//

        private fun copyFunctionDescriptor(oldDescriptor: CallableDescriptor, oldContainingDeclaration: DeclarationDescriptor) =
                when (oldDescriptor) {
                    is ConstructorDescriptor    -> copyConstructorDescriptor(oldDescriptor)
                    is SimpleFunctionDescriptor -> copySimpleFunctionDescriptor(oldDescriptor, oldContainingDeclaration)
                    else -> TODO("Unsupported FunctionDescriptor subtype: $oldDescriptor")
                }

        //---------------------------------------------------------------------//

        private fun copySimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor, oldContainingDeclaration: DeclarationDescriptor) : FunctionDescriptor {
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return SimpleFunctionDescriptorImpl.create(
                    /* containingDeclaration = */ newContainingDeclaration,
                    /* annotations           = */ oldDescriptor.annotations,
                    /* name                  = */ generateCopyName(oldDescriptor.name),
                    /* kind                  = */ oldDescriptor.kind,
                    /* source                = */ oldDescriptor.source
            )
        }

        //---------------------------------------------------------------------//

        private fun copyConstructorDescriptor(oldDescriptor: ConstructorDescriptor) : FunctionDescriptor {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return ClassConstructorDescriptorImpl.create(
                    /* containingDeclaration = */ newContainingDeclaration as ClassDescriptor,
                    /* annotations           = */ oldDescriptor.annotations,
                    /* isPrimary             = */ oldDescriptor.isPrimary,
                    /* source                = */ oldDescriptor.source
            )
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyOrField(oldDescriptor: PropertyDescriptor) {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration) as ClassDescriptor
            @Suppress("DEPRECATION")
            val newDescriptor = PropertyDescriptorImpl.create(
                    /* containingDeclaration = */ newContainingDeclaration,
                    /* annotations           = */ oldDescriptor.annotations,
                    /* modality              = */ oldDescriptor.modality,
                    /* visibility            = */ oldDescriptor.visibility,
                    /* isVar                 = */ oldDescriptor.isVar,
                    /* name                  = */ oldDescriptor.name,
                    /* kind                  = */ oldDescriptor.kind,
                    /* source                = */ oldDescriptor.source,
                    /* lateInit              = */ oldDescriptor.isLateInit,
                    /* isConst               = */ oldDescriptor.isConst,
                    /* isExpect              = */ oldDescriptor.isExpect,
                    /* isActual              = */ oldDescriptor.isActual,
                    /* isExternal            = */ oldDescriptor.isExternal,
                    /* isDelegated           = */ oldDescriptor.isDelegated
            )
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
        }

        //---------------------------------------------------------------------//

        private fun copyClassDescriptor(oldDescriptor: ClassDescriptor): ClassDescriptorImpl {
            val oldSuperClass = oldDescriptor.getSuperClassOrAny()
            val newSuperClass = descriptorSubstituteMap.getOrDefault(oldSuperClass, oldSuperClass) as ClassDescriptor
            val oldInterfaces = oldDescriptor.getSuperInterfaces()
            val newInterfaces = oldInterfaces.map { descriptorSubstituteMap.getOrDefault(it, it) as ClassDescriptor }
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            val newName = if (DescriptorUtils.isAnonymousObject(oldDescriptor))      // Anonymous objects are identified by their name.
                              oldDescriptor.name                                     // We need to preserve it for LocalDeclarationsLowering.
                          else
                              generateCopyName(oldDescriptor.name)

            val visibility = oldDescriptor.visibility

            return object : ClassDescriptorImpl(
                    /* containingDeclaration = */ newContainingDeclaration,
                    /* name                  = */ newName,
                    /* modality              = */ oldDescriptor.modality,
                    /* kind                  = */ oldDescriptor.kind,
                    /* supertypes            = */ listOf(newSuperClass.defaultType) + newInterfaces.map { it.defaultType },
                    /* source                = */ oldDescriptor.source,
                    /* isExternal            = */ oldDescriptor.isExternal,
                    /* storageManager        = */ LockBasedStorageManager.NO_LOCKS
            ) {
                override fun getVisibility() = visibility

                override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> {
                    return oldDescriptor.declaredTypeParameters
                }
            }
        }
    }

    //-------------------------------------------------------------------------//

    inner class DescriptorCollectorInitPhase : IrElementVisitorVoidWithContext() {

        private val initializedProperties = mutableSetOf<PropertyDescriptor>()

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitPropertyNew(declaration: IrProperty) {
            initPropertyOrField(declaration.descriptor)
            super.visitPropertyNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFieldNew(declaration: IrField) {
            val oldDescriptor = declaration.descriptor
            if (!initializedProperties.contains(oldDescriptor)) {
                initPropertyOrField(oldDescriptor)                                          // A field without a property or a field of a delegated property.
            }
            super.visitFieldNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunctionNew(declaration: IrFunction) {
            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) {                             // Property accessors are copied along with their property.
                val newDescriptor = initFunctionDescriptor(oldDescriptor)
                oldDescriptor.extensionReceiverParameter?.let {
                    descriptorSubstituteMap[it] = newDescriptor.extensionReceiverParameter!!
                }
            }
            super.visitFunctionNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable) {
            declaration.descriptor.let { descriptorSubstituteMap[it] = copyVariableDescriptor(it) }

            super.visitVariable(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitCatch(aCatch: IrCatch) {
            aCatch.parameter.let { descriptorSubstituteMap[it] = copyVariableDescriptor(it) }

            super.visitCatch(aCatch)
        }

        //--- Copy descriptors ------------------------------------------------//

        private fun generateCopyName(name: Name): Name {
            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(declarationName + "_" + indexStr)
        }

        //---------------------------------------------------------------------//

        private fun copyVariableDescriptor(oldDescriptor: VariableDescriptor): VariableDescriptor {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return IrTemporaryVariableDescriptorImpl(
                    containingDeclaration = newContainingDeclaration,
                    name                  = generateCopyName(oldDescriptor.name),
                    outType               = substituteType(oldDescriptor.type)!!,
                    isMutable             = oldDescriptor.isVar
            )
        }

        //---------------------------------------------------------------------//

        private fun initFunctionDescriptor(oldDescriptor: CallableDescriptor): CallableDescriptor =
                when (oldDescriptor) {
                    is ConstructorDescriptor    -> initConstructorDescriptor(oldDescriptor)
                    is SimpleFunctionDescriptor -> initSimpleFunctionDescriptor(oldDescriptor)
                    else -> TODO("Unsupported FunctionDescriptor subtype: $oldDescriptor")
                }

        //---------------------------------------------------------------------//

        private fun initSimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor): FunctionDescriptor =
                (descriptorSubstituteMap[oldDescriptor] as SimpleFunctionDescriptorImpl).apply {
                    val oldDispatchReceiverParameter = oldDescriptor.dispatchReceiverParameter
                    val newDispatchReceiverParameter = oldDispatchReceiverParameter?.let { descriptorSubstituteMap.getOrDefault(it, it) as ReceiverParameterDescriptor }
                    val newTypeParameters = oldDescriptor.typeParameters        // TODO substitute types
                    val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, this)
                    val newReceiverParameterType = substituteType(oldDescriptor.extensionReceiverParameter?.type)
                    val newReturnType = substituteType(oldDescriptor.returnType)

                    initialize(
                            /* receiverParameterType        = */ newReceiverParameterType.createExtensionReceiver(this),
                            /* dispatchReceiverParameter    = */ newDispatchReceiverParameter,
                            /* typeParameters               = */ newTypeParameters,
                            /* unsubstitutedValueParameters = */ newValueParameters,
                            /* unsubstitutedReturnType      = */ newReturnType,
                            /* modality                     = */ oldDescriptor.modality,
                            /* visibility                   = */ oldDescriptor.visibility
                    )
                    isTailrec = oldDescriptor.isTailrec
                    isSuspend = oldDescriptor.isSuspend
                    overriddenDescriptors += oldDescriptor.overriddenDescriptors
                }

        //---------------------------------------------------------------------//

        private fun initConstructorDescriptor(oldDescriptor: ConstructorDescriptor): FunctionDescriptor =
                (descriptorSubstituteMap[oldDescriptor] as ClassConstructorDescriptorImpl).apply {
                    val newTypeParameters = oldDescriptor.typeParameters
                    val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, this)
                    val receiverParameterType = substituteType(oldDescriptor.dispatchReceiverParameter?.type)
                    val returnType = substituteType(oldDescriptor.returnType)

                    initialize(
                            /* receiverParameterType        = */ receiverParameterType.createExtensionReceiver(this),
                            /* dispatchReceiverParameter    = */ null,                              //  For constructor there is no explicit dispatch receiver.
                            /* typeParameters               = */ newTypeParameters,
                            /* unsubstitutedValueParameters = */ newValueParameters,
                            /* unsubstitutedReturnType      = */ returnType,
                            /* modality                     = */ oldDescriptor.modality,
                            /* visibility                   = */ oldDescriptor.visibility
                    )
                }

        //---------------------------------------------------------------------//

        private fun initPropertyOrField(oldDescriptor: PropertyDescriptor) {
            val newDescriptor = (descriptorSubstituteMap[oldDescriptor] as PropertyDescriptorImpl).apply {
                setType(
                        /* outType                   = */ substituteType(oldDescriptor.type)!!,
                        /* typeParameters            = */ oldDescriptor.typeParameters,
                        /* dispatchReceiverParameter = */ (containingDeclaration as ClassDescriptor).thisAsReceiverParameter,
                        /* extensionReceiverParamter = */ substituteType(oldDescriptor.extensionReceiverParameter?.type).createExtensionReceiver(this))

                initialize(
                        /* getter = */ oldDescriptor.getter?.let { copyPropertyGetterDescriptor(it, this) },
                        /* setter = */ oldDescriptor.setter?.let { copyPropertySetterDescriptor(it, this) })

                overriddenDescriptors += oldDescriptor.overriddenDescriptors
            }
            oldDescriptor.getter?.let { descriptorSubstituteMap[it] = newDescriptor.getter!! }
            oldDescriptor.setter?.let { descriptorSubstituteMap[it] = newDescriptor.setter!! }
            oldDescriptor.extensionReceiverParameter?.let {
                descriptorSubstituteMap[it] = newDescriptor.extensionReceiverParameter!!
            }
            initializedProperties.add(oldDescriptor)
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyGetterDescriptor(oldDescriptor: PropertyGetterDescriptor, newPropertyDescriptor: PropertyDescriptor) =
                PropertyGetterDescriptorImpl(
                        /* correspondingProperty = */ newPropertyDescriptor,
                        /* annotations           = */ oldDescriptor.annotations,
                        /* modality              = */ oldDescriptor.modality,
                        /* visibility            = */ oldDescriptor.visibility,
                        /* isDefault             = */ oldDescriptor.isDefault,
                        /* isExternal            = */ oldDescriptor.isExternal,
                        /* isInline              = */ oldDescriptor.isInline,
                        /* kind                  = */ oldDescriptor.kind,
                        /* original              = */ null,
                        /* source                = */ oldDescriptor.source).apply {
                    initialize(substituteType(oldDescriptor.returnType))
                }

        //---------------------------------------------------------------------//

        private fun copyPropertySetterDescriptor(oldDescriptor: PropertySetterDescriptor, newPropertyDescriptor: PropertyDescriptor) =
                PropertySetterDescriptorImpl(
                        /* correspondingProperty = */ newPropertyDescriptor,
                        /* annotations           = */ oldDescriptor.annotations,
                        /* modality              = */ oldDescriptor.modality,
                        /* visibility            = */ oldDescriptor.visibility,
                        /* isDefault             = */ oldDescriptor.isDefault,
                        /* isExternal            = */ oldDescriptor.isExternal,
                        /* isInline              = */ oldDescriptor.isInline,
                        /* kind                  = */ oldDescriptor.kind,
                        /* original              = */ null,
                        /* source                = */ oldDescriptor.source).apply {
                    initialize(copyValueParameters(oldDescriptor.valueParameters, this).single())
                }

        //-------------------------------------------------------------------------//

        private fun copyValueParameters(oldValueParameters: List <ValueParameterDescriptor>, containingDeclaration: CallableDescriptor) =
                oldValueParameters.map { oldDescriptor ->
                    val newDescriptor = ValueParameterDescriptorImpl(
                            containingDeclaration = containingDeclaration,
                            original              = oldDescriptor.original,
                            index                 = oldDescriptor.index,
                            annotations           = oldDescriptor.annotations,
                            name                  = oldDescriptor.name,
                            outType               = substituteType(oldDescriptor.type)!!,
                            declaresDefaultValue  = oldDescriptor.declaresDefaultValue(),
                            isCrossinline         = oldDescriptor.isCrossinline,
                            isNoinline            = oldDescriptor.isNoinline,
                            varargElementType     = substituteType(oldDescriptor.varargElementType),
                            source                = oldDescriptor.source
                    )
                    descriptorSubstituteMap[oldDescriptor] = newDescriptor
                    newDescriptor
                }

    }

//-----------------------------------------------------------------------------//

    @Suppress("DEPRECATION")
    inner class InlineCopyIr : DeepCopyIrTree() {

        override fun mapClassDeclaration            (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapTypeAliasDeclaration        (descriptor: TypeAliasDescriptor)             = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as TypeAliasDescriptor
        override fun mapFunctionDeclaration         (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapConstructorDeclaration      (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapPropertyDeclaration         (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapLocalPropertyDeclaration    (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapEnumEntryDeclaration        (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapVariableDeclaration         (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapErrorDeclaration            (descriptor: DeclarationDescriptor)           = descriptorSubstituteMap.getOrDefault(descriptor, descriptor)

        override fun mapClassReference              (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapValueReference              (descriptor: ValueDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ValueDescriptor
        override fun mapVariableReference           (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapPropertyReference           (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapCallee                      (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapDelegatedConstructorCallee  (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapEnumConstructorCallee       (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapLocalPropertyReference      (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapClassifierReference         (descriptor: ClassifierDescriptor)            = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassifierDescriptor
        override fun mapReturnTarget                (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor

        //---------------------------------------------------------------------//

        override fun mapSuperQualifier(qualifier: ClassDescriptor?): ClassDescriptor? {
            if (qualifier == null) return null
            return descriptorSubstituteMap.getOrDefault(qualifier,  qualifier) as ClassDescriptor
        }

        //--- Visits ----------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrCall {
            if (expression !is IrCallImpl) return super.visitCall(expression)
            val newDescriptor = mapCallee(expression.descriptor)
            return IrCallImpl(
                startOffset    = expression.startOffset,
                endOffset      = expression.endOffset,
                type           = context.symbolTable.translateErased(newDescriptor.returnType!!),
                descriptor     = newDescriptor,
                typeArgumentsCount = expression.typeArgumentsCount,
                origin         = expression.origin,
                superQualifierDescriptor = mapSuperQualifier(expression.superQualifier)
            ).apply {
                transformValueArguments(expression)
                substituteTypeArguments(expression)
            }
        }

        override fun visitField(declaration: IrField): IrField {
            val descriptor = mapPropertyDeclaration(declaration.descriptor)
            return IrFieldImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    descriptor,
                    context.symbolTable.translateErased(descriptor.type),
                    declaration.initializer?.transform(this@InlineCopyIr, null)
            ).apply {
                transformAnnotations(declaration)
            }
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction) =
            context.symbolTable.withScope(mapFunctionDeclaration(declaration.descriptor)) { descriptor ->
                IrFunctionImpl(
                    startOffset = declaration.startOffset,
                    endOffset = declaration.endOffset,
                    origin = mapDeclarationOrigin(declaration.origin),
                    descriptor = descriptor,
                    returnType = declaration.returnType,
                    body = declaration.body?.transform(this@InlineCopyIr, null)
                ).also {
                    it.returnType = context.symbolTable.translateErased(descriptor.returnType!!)
                    it.setOverrides(context.symbolTable)
                    it.transformParameters(declaration)
                }
            }

//        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrFunction {
//            val descriptor = mapFunctionDeclaration(declaration.descriptor)
//            return IrFunctionImpl(
//                startOffset = declaration.startOffset,
//                endOffset   = declaration.endOffset,
//                origin      = mapDeclarationOrigin(declaration.origin),
//                descriptor  = descriptor
//            ).also {
//                it.returnType = context.symbolTable.translateErased(descriptor.returnType!!)
//                it.body = declaration.body?.transform(this, null)
//
//                it.setOverrides(context.symbolTable)
//            }.transformParameters1(declaration)
//        }

//        override fun visitConstructor(declaration: IrConstructor): IrConstructor {
//            val descriptor = mapConstructorDeclaration(declaration.descriptor)
//            return IrConstructorImpl(
//                startOffset = declaration.startOffset,
//                endOffset   = declaration.endOffset,
//                origin      = mapDeclarationOrigin(declaration.origin),
//                descriptor  = descriptor
//            ).also {
//                it.returnType = context.symbolTable.translateErased(descriptor.returnType)
//                it.body = declaration.body?.transform(this, null)
//            }.transformParameters1(declaration)
//        }

        private fun FunctionDescriptor.getTypeParametersToTransform() =
            when {
                this is PropertyAccessorDescriptor -> correspondingProperty.typeParameters
                else -> typeParameters
            }

        protected fun <T : IrFunction> T.transformParameters1(original: T): T =
            apply {
                transformTypeParameters(original, descriptor.getTypeParametersToTransform())
                transformValueParameters1(original)
            }

        protected fun <T : IrFunction> T.transformValueParameters1(original: T) =
            apply {
                dispatchReceiverParameter =
                        original.dispatchReceiverParameter?.replaceDescriptor1(
                            descriptor.dispatchReceiverParameter ?: throw AssertionError("No dispatch receiver in $descriptor")
                        )

                extensionReceiverParameter =
                        original.extensionReceiverParameter?.replaceDescriptor1(
                            descriptor.extensionReceiverParameter ?: throw AssertionError("No extension receiver in $descriptor")
                        )

                original.valueParameters.mapIndexedTo(valueParameters) { i, originalValueParameter ->
                    originalValueParameter.replaceDescriptor1(descriptor.valueParameters[i])
                }
            }

        protected fun IrValueParameter.replaceDescriptor1(newDescriptor: ParameterDescriptor) =
            IrValueParameterImpl(
                startOffset, endOffset,
                mapDeclarationOrigin(origin),
                newDescriptor,
                context.symbolTable.translateErased(newDescriptor.type),
                (newDescriptor as? ValueParameterDescriptor)?.varargElementType?.let { context.symbolTable.translateErased(it) },
                defaultValue?.transform(this@InlineCopyIr, null)
            ).apply {
                transformAnnotations(this)
            }

        //---------------------------------------------------------------------//

        override fun visitGetValue(expression: IrGetValue): IrGetValue {
            val descriptor = mapValueReference(expression.descriptor)
            return IrGetValueImpl(
                    expression.startOffset, expression.endOffset,
                    context.symbolTable.translateErased(descriptor.type),
                    descriptor,
                    mapStatementOrigin(expression.origin)
            )
        }

        override fun visitVariable(declaration: IrVariable): IrVariable {
            val descriptor = mapVariableDeclaration(declaration.descriptor)
            return IrVariableImpl(
                    declaration.startOffset, declaration.endOffset,
                    mapDeclarationOrigin(declaration.origin),
                    descriptor,
                    context.symbolTable.translateErased(descriptor.type),
                    declaration.initializer?.transform(this, null)
            ).apply {
                transformAnnotations(declaration)
            }
        }

        private fun <T : IrFunction> T.transformDefaults(original: T): T {
            for (originalValueParameter in original.descriptor.valueParameters) {
                val valueParameter = descriptor.valueParameters[originalValueParameter.index]
                original.getDefault(originalValueParameter)?.let { irDefaultParameterValue ->
                    putDefault(valueParameter, irDefaultParameterValue.transform(this@InlineCopyIr, null))
                }
            }
            return this
        }

        //---------------------------------------------------------------------//

        fun getTypeOperatorReturnType(operator: IrTypeOperator, type: IrType) : IrType {
            return when (operator) {
                IrTypeOperator.CAST,
                IrTypeOperator.IMPLICIT_CAST,
                IrTypeOperator.IMPLICIT_NOTNULL,
                IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                IrTypeOperator.IMPLICIT_INTEGER_COERCION,
                IrTypeOperator.SAM_CONVERSION               -> type
                IrTypeOperator.SAFE_CAST                    -> type.makeNullable()
                IrTypeOperator.INSTANCEOF,
                IrTypeOperator.NOT_INSTANCEOF               -> context.irBuiltIns.booleanType
            }
        }

        //---------------------------------------------------------------------//

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall {
            val erasedTypeOperand = substituteAndEraseType(expression.typeOperand)!!
            val typeOperand = substituteAndBreakType(expression.typeOperand)
            val returnType = getTypeOperatorReturnType(expression.operator, erasedTypeOperand)
            return IrTypeOperatorCallImpl(
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = returnType,
                operator    = expression.operator,
                typeOperand = typeOperand,
                argument    = expression.argument.transform(this, null),
                typeOperandClassifier = (typeOperand as IrSimpleType).classifier
            )
        }

        //---------------------------------------------------------------------//

        override fun visitReturn(expression: IrReturn): IrReturn =
            IrReturnImpl(
                startOffset  = expression.startOffset,
                endOffset    = expression.endOffset,
                type         = substituteAndEraseType(expression.type)!!,
                returnTargetDescriptor = mapReturnTarget(expression.returnTarget),
                value        = expression.value.transform(this, null)
            )

        //---------------------------------------------------------------------//

        override fun visitBlock(expression: IrBlock): IrBlock {
            return if (expression is IrReturnableBlock) {
                IrReturnableBlockImpl(
                    startOffset    = expression.startOffset,
                    endOffset      = expression.endOffset,
                    type           = expression.type,
                    descriptor     = expression.descriptor,
                    origin         = mapStatementOrigin(expression.origin),
                    statements     = expression.statements.map { it.transform(this, null) },
                    sourceFileName = expression.sourceFileName
                )
            } else {
                IrBlockImpl(
                        expression.startOffset, expression.endOffset,
                        substituteAndEraseType(expression.type)!!,
                        mapStatementOrigin(expression.origin),
                        expression.statements.map { it.transform(this, null) }
                )
            }
        }

        //-------------------------------------------------------------------------//

        override fun visitClassReference(expression: IrClassReference): IrClassReference {
            val newExpressionType = substituteAndEraseType(expression.type)!!                       // Substituted expression type.
            val newDescriptorType = substituteType(expression.descriptor.defaultType)!!     // Substituted type of referenced class.
            val classDescriptor = newDescriptorType.constructor.declarationDescriptor!!     // Get ClassifierDescriptor of the referenced class.
            return IrClassReferenceImpl(
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = newExpressionType,
                descriptor  = classDescriptor,
                classType   = expression.classType
            )
        }

        //-------------------------------------------------------------------------//

        override fun visitGetClass(expression: IrGetClass): IrGetClass {
            val type = substituteAndEraseType(expression.type)!!
            return IrGetClassImpl(
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = type,
                argument    = expression.argument.transform(this, null)
            )
        }

        //-------------------------------------------------------------------------//

        override fun getNonTransformedLoop(irLoop: IrLoop): IrLoop {
            return irLoop
        }

        override fun visitClass(declaration: IrClass): IrClass {
            val descriptor = this.mapClassDeclaration(declaration.descriptor)

            return context.symbolTable.declareClass(
                    declaration.startOffset, declaration.endOffset, mapDeclarationOrigin(declaration.origin),
                    descriptor
            ).apply {
                declaration.declarations.mapTo(this.declarations) {
                    it.transform(this@InlineCopyIr, null) as IrDeclaration
                }
                this.transformAnnotations(declaration)
                this.thisReceiver = declaration.thisReceiver?.replaceDescriptor1(this.descriptor.thisAsReceiverParameter)

                this.transformTypeParameters(declaration, this.descriptor.declaredTypeParameters)

                descriptor.defaultType.constructor.supertypes.mapTo(this.superTypes) {
                    context.symbolTable.translateErased(it)
                }
            }
        }
    }

    //-------------------------------------------------------------------------//

    private fun substituteType(type: KotlinType?): KotlinType? {
        val substitutedType = (type?.let { typeSubstitutor?.substitute(it, Variance.INVARIANT) } ?: type)
                ?: return null
        val oldClassDescriptor = TypeUtils.getClassDescriptor(substitutedType) ?: return substitutedType
        return descriptorSubstituteMap[oldClassDescriptor]?.let { (it as ClassDescriptor).defaultType } ?: substitutedType
    }

    private fun substituteAndEraseType(oldType: IrType?): IrType? {
        oldType ?: return null

        val substitutedKotlinType = substituteType(oldType.toKotlinType())
                ?: return oldType
        return context.symbolTable.translateErased(substitutedKotlinType)
    }

    fun translateBroken(type: KotlinType): IrType {
        val declarationDescriptor = type.constructor.declarationDescriptor
        return when (declarationDescriptor) {
            is ClassDescriptor -> {
                val classifier = context.symbolTable.referenceClassifier(declarationDescriptor)
                val typeArguments = type.arguments.map {
                    if (it.isStarProjection) {
                        IrStarProjectionImpl
                    } else {
                        makeTypeProjection(translateBroken(it.type), it.projectionKind)
                    }
                }
                IrSimpleTypeImpl(
                    classifier,
                    type.isMarkedNullable,
                    typeArguments,
                    emptyList()
                )
            }
            is TypeParameterDescriptor -> IrSimpleTypeImpl(
                context.symbolTable.referenceTypeParameter(declarationDescriptor),
                type.isMarkedNullable,
                emptyList(),
                emptyList()
            )
            else -> error(declarationDescriptor ?: "null")
        }
    }

    private fun substituteAndBreakType(oldType: IrType): IrType {
        return translateBroken(substituteType(oldType.toKotlinType())!!)
    }

    //-------------------------------------------------------------------------//

    private fun IrMemberAccessExpression.substituteTypeArguments(original: IrMemberAccessExpression) {
        for (index in 0 until original.typeArgumentsCount) {
            val originalTypeArgument = original.getTypeArgument(index)
            val newTypeArgument = substituteAndBreakType(originalTypeArgument!!)
            this.putTypeArgument(index, newTypeArgument)
        }
    }

    //-------------------------------------------------------------------------//

    override fun addCurrentSubstituteMap(globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>) {
        descriptorSubstituteMap.forEach { t, u ->
            globalSubstituteMap[t] = SubstitutedDescriptor(targetDescriptor, u)
        }
    }

}

class SubstitutedDescriptor(val inlinedFunction: FunctionDescriptor, val descriptor: DeclarationDescriptor)

internal class DescriptorSubstitutorForExternalScope(
        val globalSubstituteMap: Map<DeclarationDescriptor, SubstitutedDescriptor>,
        val context: Context
)
    : IrElementTransformerVoidWithContext() {

    fun run(element: IrElement) {
        element.transformChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val oldExpression = super.visitCall(expression) as IrCall

        val substitutedDescriptor = globalSubstituteMap[expression.descriptor.original]
            ?: return oldExpression
        if (allScopes.any { it.scope.scopeOwner == substitutedDescriptor.inlinedFunction })
            return oldExpression
        return when (oldExpression) {
            is IrCallImpl -> copyIrCallImpl(oldExpression, substitutedDescriptor)
            is IrCallWithShallowCopy -> copyIrCallWithShallowCopy(oldExpression, substitutedDescriptor)
            else -> oldExpression
        }
    }

    //-------------------------------------------------------------------------//

    private fun copyIrCallImpl(oldExpression: IrCallImpl, substitutedDescriptor: SubstitutedDescriptor): IrCallImpl {

        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = substitutedDescriptor.descriptor as FunctionDescriptor

        if (newDescriptor == oldDescriptor)
            return oldExpression

        return IrCallImpl(
            startOffset              = oldExpression.startOffset,
            endOffset                = oldExpression.endOffset,
            type                     = context.symbolTable.translateErased(newDescriptor.returnType!!),
            symbol                   = createFunctionSymbol(newDescriptor),
            descriptor               = newDescriptor,
            typeArgumentsCount       = oldExpression.typeArgumentsCount,
            origin                   = oldExpression.origin,
            superQualifierSymbol     = createClassSymbolOrNull(oldExpression.superQualifier)
        ).apply {
            copyTypeArgumentsFrom(oldExpression)

            oldExpression.descriptor.valueParameters.forEach {
                val valueArgument = oldExpression.getValueArgument(it)
                putValueArgument(it.index, valueArgument)
            }
            extensionReceiver = oldExpression.extensionReceiver
            dispatchReceiver  = oldExpression.dispatchReceiver
        }
    }

    //-------------------------------------------------------------------------//

    private fun copyIrCallWithShallowCopy(oldExpression: IrCallWithShallowCopy, substitutedDescriptor: SubstitutedDescriptor): IrCall {

        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = substitutedDescriptor.descriptor as FunctionDescriptor

        if (newDescriptor == oldDescriptor)
            return oldExpression

        return oldExpression.shallowCopy(oldExpression.origin, createFunctionSymbol(newDescriptor), oldExpression.superQualifierSymbol)
    }
}