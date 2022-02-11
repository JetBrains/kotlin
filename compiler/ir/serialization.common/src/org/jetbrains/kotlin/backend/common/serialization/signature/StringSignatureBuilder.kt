/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

import org.jetbrains.kotlin.backend.common.serialization.isExpectMember
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.OptionalAnnotationUtil
import org.jetbrains.kotlin.resolve.multiplatform.findCompatibleActualsForExpected
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

private fun Char.isValidId(): Boolean {
    return this == '_' || this.isLetterOrDigit()
}

private fun Char.isValidIdStart(): Boolean {
    return this == '_' || this.isLetter()
}

private fun StringBuilder.escapeIdentifier(ident: String) {
//
//    append(ident)
//    return

    var i = 0

//    if (ident == "\$metadata\$") {
//        println("ll")
//    }

    while (i < ident.length) {
        val c = ident[i]
        val dontEscape = if (i == 0) {
            c.isValidIdStart()
        } else c.isValidId()

        if (!dontEscape) {
            append('\\')
        }
        append(c)
        ++i
    }
}

class StringSignatureBuilderOverIr(
    private val localClassResolver: ((IrDeclaration) -> Int)? = null // for local class index resolution
) {

    fun inFile(file: IrFile?, block: () -> Unit) {
        block()
    }

    private fun skipLocalDeclaration(declaration: IrDeclaration): Boolean {
        if (declaration is IrDeclarationWithName) {
            if (declaration.name == SpecialNames.ANONYMOUS) return true
        }
        if (declaration.isLocal) {
            if (localClassResolver != null) {
                if (declaration is IrDeclarationWithVisibility) {
                    if (declaration.visibility == DescriptorVisibilities.LOCAL) return true
                    if (declaration.visibility != DescriptorVisibilities.PRIVATE) return false
                }
            }
            return true
        }
        return false
    }

    fun buildForDeclaration(declaration: IrDeclaration): StringSignature? {

        if (skipLocalDeclaration(declaration)) return null

        val string = buildString {
            when (declaration) {
                is IrSimpleFunction -> buildForFunction(declaration)
                is IrClass -> buildForClass(declaration)
                is IrTypeAlias -> buildForTypeAlias(declaration)
                is IrEnumEntry -> buildForEnumEntry(declaration)
                is IrProperty -> buildForProperty(declaration)
                is IrField -> buildForField(declaration)
                is IrTypeParameter -> buildForTypeParameter(declaration)
                is IrConstructor -> buildForConstructor(declaration)
                is IrValueDeclaration -> return null
                is IrAnonymousInitializer -> return null
                is IrLocalDelegatedProperty -> return null
                is IrErrorDeclaration -> return null
                else -> error("Unexpected type of declaration (${declaration.render()})")
            }
        }

        return StringSignature(string)
    }

    private fun StringBuilder.addParentName(parent: IrDeclarationParent, visibility: DescriptorVisibility, needFile: Boolean) {
        when (parent) {
            is IrFile -> {
                if (visibility == DescriptorVisibilities.PRIVATE && needFile) {
                    append(File(parent.fileEntry.name).nameWithoutExtension)
                    append('/')
                }
                append(parent.fqName.asString())
                append('/')
            }
            is IrPackageFragment -> {
                append(parent.fqName.asString())
                append('/')
            }
            else -> {
                val decl = parent as IrDeclarationWithName

                localClassResolver?.let { resolver ->
                    if (decl is IrClass) {
                        if (decl.isLocal) {
                            val id = resolver(decl)
                            append(MangleConstant.LOCAL_CLASS_MARK)
                            append(id)
                            return
                        }
                    }
                }

                val pVisibility = (decl as? IrDeclarationWithVisibility)?.visibility ?: DescriptorVisibilities.PUBLIC
                val l = length
                addParentName(decl.parent, pVisibility, needFile)
                if (last() != '/' && l != length) {
                    append('.')
                }
                escapeIdentifier(decl.name.asString())
            }
        }
    }

    private fun StringBuilder.buildForTypeAlias(typeAlias: IrTypeAlias) {
        val l = length
        addParentName(typeAlias.parent, typeAlias.visibility, needFile = false)
        if (last() != '/' && l != length) {
            append('.')
        }
        escapeIdentifier(typeAlias.name.asString())
    }

    private fun StringBuilder.buildForEnumEntry(enumEntry: IrEnumEntry) {
        val l = length
        addParentName(enumEntry.parent, DescriptorVisibilities.DEFAULT_VISIBILITY, needFile = false)
        if (last() != '/' && l != length) {
            append('.')
        }
        escapeIdentifier(enumEntry.name.asString())
    }

    /**
     * ClassSignature:
     *   Fqn ‘/’ Fqn
     *   ‘$’ #num // for local classes
     */
    private fun StringBuilder.buildForClass(klass: IrClass) {
        localClassResolver?.let { resolver ->
            if (klass.isLocal) {
                val id = resolver(klass)
                append(MangleConstant.LOCAL_CLASS_MARK)
                append(id)
                return
            }
        }

        val l = length
        addParentName(klass.parent, klass.visibility, needFile = false)
        if (last() != '/' && l != length) {
            append('.')
        }
        escapeIdentifier(klass.name.asString())
        if (klass.kind == ClassKind.ENUM_ENTRY) {
            append('.')
            append("EEC")
        }
    }

    /**
     * FieldSignature:
     *   PropertySignature ‘§’  // backing field of property
     *   ClassSignature ‘§’ ID  // any other field
     */
    private fun StringBuilder.buildForField(field: IrField) {
        field.correspondingPropertySymbol?.let {
            buildForProperty(it.owner)
            append(MangleConstant.FIELD_MARK)
        } ?: run {
            val klass = field.parent as? IrClass ?: error("Non-backing field has to have IrClass as a parent (${field.render()})")
            buildForClass(klass)
            append(MangleConstant.FIELD_MARK)
            escapeIdentifier(field.name.asString())
        }
    }

    /**
     * PropertySignature:
     *   ( ID ‘/’ ) Fqn ‘/’ Fqn PropertyTypeSignature TypeParameters?
     */
    private fun StringBuilder.buildForProperty(property: IrProperty) {
        val l = length
        addParentName(property.parent, property.visibility, needFile = property.parent is IrPackageFragment)
        if (last() != '/' && l != length) {
            append('.')
        }
        escapeIdentifier(property.name.asString())
        val accessor = property.getter ?: error("No get accessor for property")
        buildPropertyTypeSignature(accessor, property)
        buildTypeParameters(accessor)
    }

    /**
     * PropertyTypeSignature:
     *   ExtensionReceiverType? ReturnType
     */
    private fun StringBuilder.buildPropertyTypeSignature(accessor: IrSimpleFunction, property: IrProperty) {
        assert(accessor.correspondingPropertySymbol != null)

        accessor.extensionReceiverParameter?.let {
            buildExtensionReceiver(it, property)
        }

        append('=')
        buildType(accessor.returnType, property)
    }


    /**
     * TypeParameterSignature:
     *   TypeParameterContainer ‘|’ #num
     *
     * TypeParameterContainer:
     *   ClassSignature
     *   PropertySignature
     *   MethodSignature
     */
    private fun StringBuilder.buildForTypeParameter(typeParameter: IrTypeParameter) {
        val container = typeParameter.parent

        var isSetter = false
        when (container) {
            is IrClass -> buildForClass(container)
            is IrTypeAlias -> buildForTypeAlias(container)
            is IrConstructor -> buildForConstructor(container)
            is IrSimpleFunction -> {
                container.correspondingPropertySymbol?.let {
                    buildForProperty(it.owner)
                    isSetter = it.owner.setter === container && it.owner.getter !== container
                } ?: buildForFunction(container)
            }
        }

        append('|')
        append(typeParameter.index)
        if (isSetter) append('S')
    }

    /**
     * MethodSignature:
     *   ( ID ‘/’ ) Fqn ‘/’ Fqn? ‘.’ FunctionName MethodTypeSignature TypeParameters? ( ‘|’ ‘S’ )?
     */
    private fun StringBuilder.buildForFunction(function: IrSimpleFunction) {

        val isTopLevel = function.parent is IrPackageFragment
        val l = length
        function.correspondingPropertySymbol?.let { propertySymbol ->
            val property = propertySymbol.owner
            val name = if (function === property.getter) MangleConstant.GETTER_MARK else MangleConstant.SETTER_MARK
            addParentName(property.parent, property.visibility, isTopLevel)
            if (last() != '/' && l != length) {
                append('.')
            }
            escapeIdentifier(property.name.asString())
            append('.')
            append(name)
        } ?: run {
            addParentName(function.parent, function.visibility, isTopLevel)
            if (last() != '/' && l != length) {
                append('.')
            }
            escapeIdentifier(function.name.asString())
        }

        buildMethodTypeSignature(function)
        buildTypeParameters(function)
        if (function.isSuspend) {
            append('|')
            append(MangleConstant.SUSPEND_MARK)
        }
    }

    private fun StringBuilder.buildForConstructor(constructor: IrConstructor) {
        addParentName(constructor.parent, constructor.visibility, needFile = false)
        append('.')
        append(MangleConstant.CONSTRUCTOR_MARK)
        buildMethodTypeSignature(constructor)
        buildTypeParameters(constructor)
    }

    /**
     * MethodTypeSignature:
     *   ExtensionReceiverType? ‘(’ ParameterTypes? ‘)’ ReturnType
     */
    private fun StringBuilder.buildMethodTypeSignature(function: IrFunction) {
        function.extensionReceiverParameter?.let {
            buildExtensionReceiver(it, function)
        }

        function.valueParameters.collectForMangler(this, MangleConstant.VALUE_PARAMETERS) { parameter ->
            /**
             * ParameterTypes:
             *   ParameterType ( ‘;’ ParameterType  )*
             *
             * ParameterType:
             *   Type ‘...’?
             */
            buildType(parameter.type, function)
            if (parameter.isVararg) {
                append(MangleConstant.VAR_ARG_MARK)
            }
        }

        /**
         * ReturnType:
         *   '=' Type
         *   // void/unit for JVM purpose ??
         */
        append(MangleConstant.RETURN_TYPE_MARK)
        buildType(function.returnType, function)
    }

    /**
     * ExtensionReceiverType:
     *   ‘@’ Type ‘@‘
     */
    private fun StringBuilder.buildExtensionReceiver(extension: IrValueParameter, declaration: IrDeclaration) {
        assert(extension.index == -1)
        append(MangleConstant.EXTENSION_RECEIVER_MARK)
        buildType(extension.type, declaration)
        append(MangleConstant.EXTENSION_RECEIVER_MARK)
    }

    /**
     * TypeParameters:
     *   ‘{’ TypeParameter ( ‘;’ TypeParameter )* ‘}’
     */
    private fun StringBuilder.buildTypeParameters(typeParametersContainer: IrTypeParametersContainer) {
        if (typeParametersContainer.typeParameters.isEmpty()) return

        typeParametersContainer.typeParameters.collectForMangler(this, MangleConstant.TYPE_PARAMETERS) { parameter ->

            /**
             * TypeParameter:
             *   Variance? ‘<’ UpperBounds ‘>’
             */

            buildVariance(parameter.variance)
            buildUpperBounds(parameter.superTypes, typeParametersContainer)
        }
    }

    /**
     * Variance:
     *   ‘+’
     *   ‘-’
     */
    private fun StringBuilder.buildVariance(variance: Variance) {
        when (variance) {
            Variance.IN_VARIANCE -> append('-')
            Variance.INVARIANT -> append("")
            Variance.OUT_VARIANCE -> append('+')
        }
    }

    /**
     * UpperBounds:
     *   UpperBound ( ‘&’ UpperBound )*
     *
     * UpperBound:
     *   Type
     */
    private fun StringBuilder.buildUpperBounds(upperBounds: List<IrType>, declaration: IrDeclaration) {
        upperBounds.map { buildString { buildType(it, declaration) } }.sorted()
            .collectForMangler(this, MangleConstant.UPPER_BOUNDS) { append(it) }
    }

    /**
     * Type:
     *   TypeConstructor TypeArguments? Nullability?
     *   ‘^d’ // stands for dynamic type
     *   ‘^e’ // stands for error type
     */
    private fun StringBuilder.buildType(type: IrType, declaration: IrDeclaration) {
        when (type) {
            is IrDynamicType -> append(MangleConstant.DYNAMIC_TYPE_MARK)
            is IrErrorType -> append(MangleConstant.ERROR_TYPE_MARK)
            is IrSimpleType -> {

                buildTypeConstructor(type.classifier, declaration)
                buildTypeArguments(type.classifier, type.arguments, declaration)

                if (type.hasQuestionMark) {
                    append('?')
                }
            }
            else -> error("Unexpected type in Signature builder ${type.render()}")
        }
    }

    /**
     * TypeConstructor:
     *   ClassSignature
     *   TypeParameterRef
     */
    private fun StringBuilder.buildTypeConstructor(classifier: IrClassifierSymbol, declaration: IrDeclaration) {
        when (classifier) {
            is IrClassSymbol -> buildForClass(classifier.owner)
            is IrTypeParameterSymbol -> {
                val typeParameter = classifier.owner
                append('{')
                append(typeParameter.typeParameterIndex(declaration))
                append(':')
                append(typeParameter.index)
                append('}')
            }
            else -> error("Unexpected classifier $classifier")
        }
    }

    private fun IrTypeParameter.typeParameterIndex(declaration: IrDeclaration): Int {
        val tpParent = parent
        var current: IrDeclaration = declaration
        var i = 0
        while (current !is IrPackageFragment) {

            if (current is IrProperty) {
                if (tpParent === current.getter || tpParent === current.setter) return i
            }

            if (current === tpParent) return i
            ++i
            current = current.parent as? IrDeclaration ?: break
        }

        error("No type parameter parent found for ${render()} in hierarchy of ${declaration.render()}")
    }

    private fun extractTypeParameters(parent: IrDeclarationParent): List<IrTypeParameter> {
        val result = mutableListOf<IrTypeParameter>()
        var current: IrDeclarationParent? = parent
        while (current != null) {
            if (current is IrField) {
                current.correspondingPropertySymbol?.let { propS ->
                    val prop = propS.owner
                    result += (prop.getter ?: prop.setter ?: error("No accessor in property ${prop.render()}")).typeParameters
                }
            } else {
                (current as? IrTypeParametersContainer)?.let { result += it.typeParameters }
            }
            current = when (current) {
                is IrField -> current.parent
                is IrClass -> when {
                    current.isInner -> current.parent as IrClass
                    current.visibility == DescriptorVisibilities.LOCAL -> current.parent
                    else -> null
                }
                is IrConstructor -> current.parent as IrClass
                is IrFunction ->
                    if (current.visibility == DescriptorVisibilities.LOCAL || current.dispatchReceiverParameter != null)
                        current.parent
                    else
                        null
                else -> null
            }
        }
        return result
    }


    /**
     * TypeArguments:
     *   ‘<’ TypeArgument ( ‘,’ TypeArgument )* ‘>’
     *
     * TypeArgument:
     *   Variance? Type
     *   ‘*’
     */
    private fun StringBuilder.buildTypeArguments(
        classifier: IrClassifierSymbol,
        arguments: List<IrTypeArgument>,
        declaration: IrDeclaration
    ) {
        if (arguments.isEmpty()) return

        val classSymbol = classifier as? IrClassSymbol ?: error("Non-empty type arguments for non-class type constructor $classifier")
        val klass = classSymbol.owner

        val typeParameters = extractTypeParameters(klass)

        var index = 0

        arguments.collectForMangler(this, MangleConstant.TYPE_ARGUMENTS) { argument ->
            if (argument is IrTypeProjection) {
                val typeParameter = typeParameters[index]
                val effectiveVariance = effectiveVariance(typeParameter.variance, argument.variance)
                buildVariance(effectiveVariance)
                buildType(argument.type, declaration)
            } else {
                append(MangleConstant.STAR_MARK)
            }
            ++index
        }
    }

    private fun effectiveVariance(pVariance: Variance, aVariance: Variance): Variance {
        if (pVariance == Variance.INVARIANT) return aVariance

        return pVariance
    }
}

class StringSignatureBuilderOverDescriptors : StringSignatureComposer {


    override fun composeAnonInitSignature(descriptor: ClassDescriptor): StringSignature? = null

    override fun composeEnumEntrySignature(descriptor: ClassDescriptor): StringSignature? {
        return buildForDeclaration(descriptor)
    }

    override fun composeFieldSignature(descriptor: PropertyDescriptor): StringSignature? = null

    override fun composeSignature(descriptor: DeclarationDescriptor): StringSignature? {
        return buildForDeclaration(descriptor)
    }

    private fun DeclarationDescriptor.mostTopLevel(): DeclarationDescriptor {
        var d: DeclarationDescriptor = this
        while (d.containingDeclaration !is PackageFragmentDescriptor) {
            d = d.containingDeclaration ?: error("Should not be null for non-top-level")
        }

        return d
    }

    private fun MemberDescriptor.findActualForExpect(): MemberDescriptor? {
        return findCompatibleActualsForExpected(module).singleOrNull()
    }

    private fun shouldSkipExpect(descriptor: DeclarationDescriptor): Boolean {
        if (!isExpect(descriptor)) return false

        val effective = if (descriptor is PropertyAccessorDescriptor) descriptor.correspondingProperty else descriptor
        val tl = effective.mostTopLevel()

        if (tl.annotations.hasAnnotation(OptionalAnnotationUtil.OPTIONAL_EXPECTATION_FQ_NAME)) {
            tl.cast<MemberDescriptor>().findActualForExpect() ?: return false
        }
        return true
    }

    private fun isExpect(descriptor: DeclarationDescriptor): Boolean {
        if (descriptor is PropertyAccessorDescriptor) return isExpect(descriptor.correspondingProperty)
        return descriptor.isExpectMember || descriptor.containingDeclaration?.isExpectMember == true // due to FO
    }

    private fun buildForDeclaration(declaration: DeclarationDescriptor): StringSignature? {

        if (DescriptorUtils.isLocal(declaration)) return null
        if (declaration is DeclarationDescriptorWithVisibility) {
            if (declaration.visibility == DescriptorVisibilities.PRIVATE) return null
        }
        if (shouldSkipExpect(declaration)) return null

        val string = buildString {
            when (declaration) {
                is ClassConstructorDescriptor -> buildForConstructor(declaration)
                is FunctionDescriptor -> buildForFunction(declaration)
                is ClassifierDescriptorWithTypeParameters -> buildForClass(declaration)
                is PropertyDescriptor -> buildForProperty(declaration)
                is TypeParameterDescriptor -> buildForTypeParameter(declaration)
                is PackageFragmentDescriptor -> return null
                else -> error("Unexpected type of declaration ($declaration)")
            }
        }

        return StringSignature(string)
    }

    private fun StringBuilder.addParentName(parent: DeclarationDescriptor?) {
        if (parent == null) return

        when (parent) {
            is PackageFragmentDescriptor -> {
                append(parent.fqName.asString())
                append('/')
            }
            else -> {
                val l = length
                addParentName(parent.containingDeclaration)
                if (!isEmpty() && last() != '/' && l != length) {
                    append('.')
                }
                escapeIdentifier(parent.name.asString())
            }
        }
    }

    /**
     * ClassSignature:
     *   Fqn ‘/’ Fqn
     *   ‘$’ #num // for local classes
     */
    private fun StringBuilder.buildForClass(klass: ClassifierDescriptorWithTypeParameters) {
        val l = length
        addParentName(klass.containingDeclaration)
        if (last() != '/' && l != length) {
            append('.')
        }
        escapeIdentifier(klass.name.asString())
    }


    /**
     * PropertySignature:
     *   ( ID ‘/’ ) Fqn ‘/’ Fqn PropertyTypeSignature TypeParameters?
     */
    private fun StringBuilder.buildForProperty(property: PropertyDescriptor) {
        val l = length
        addParentName(property.containingDeclaration)
        if (last() != '/' && l != length) {
            append('.')
        }
        escapeIdentifier(property.name.asString())
        buildPropertyTypeSignature(property)
        buildTypeParameters(property)
    }

    /**
     * PropertyTypeSignature:
     *   ExtensionReceiverType? ReturnType
     */
    private fun StringBuilder.buildPropertyTypeSignature(property: PropertyDescriptor) {

        property.extensionReceiverParameter?.let {
            buildExtensionReceiver(it, property)
        }

        append(MangleConstant.RETURN_TYPE_MARK)
        buildType(property.type, property)
    }


    /**
     * TypeParameterSignature:
     *   TypeParameterContainer ‘|’ #num
     *
     * TypeParameterContainer:
     *   ClassSignature
     *   PropertySignature
     *   MethodSignature
     */
    private fun StringBuilder.buildForTypeParameter(typeParameter: TypeParameterDescriptor) {
        val container = typeParameter.containingDeclaration

        when (container) {
            is ClassifierDescriptorWithTypeParameters -> buildForClass(container)
            is ClassConstructorDescriptor -> buildForConstructor(container)
            is FunctionDescriptor -> buildForFunction(container)
            is PropertyDescriptor -> buildForProperty(container)
        }

        append('|')
        append(typeParameter.index)
//        if (isSetter) append('S')
    }

    /**
     * MethodSignature:
     *   ( ID ‘/’ ) Fqn ‘/’ Fqn? ‘.’ FunctionName MethodTypeSignature TypeParameters? ( ‘|’ ‘S’ )?
     */
    private fun StringBuilder.buildForFunction(function: FunctionDescriptor) {

        val isTopLevel = function.containingDeclaration is PackageFragmentDescriptor

        val containingDeclaration = if (function is PropertyAccessorDescriptor) function.correspondingProperty else function.containingDeclaration

        val l = length
        addParentName(containingDeclaration)
        if (last() != '/' && l != length) {
            append('.')
        }

        when (function) {
            is PropertyGetterDescriptor -> append(MangleConstant.GETTER_MARK)
            is PropertySetterDescriptor -> append(MangleConstant.SETTER_MARK)
            else -> escapeIdentifier(function.name.asString())
        }

        buildMethodTypeSignature(function)

        val tpContainer = if (function is PropertyAccessorDescriptor) function.correspondingProperty else function

        buildTypeParameters(tpContainer)
        if (function.isSuspend) {
            append('|')
            append(MangleConstant.SUSPEND_MARK)
        }
    }

    private fun StringBuilder.buildForConstructor(constructor: ClassConstructorDescriptor) {
        addParentName(constructor.containingDeclaration)
        append('.')
        append(MangleConstant.CONSTRUCTOR_MARK)
        buildMethodTypeSignature(constructor)
        buildTypeParameters(constructor)
    }

    /**
     * MethodTypeSignature:
     *   ExtensionReceiverType? ‘(’ ParameterTypes? ‘)’ ReturnType
     */
    private fun StringBuilder.buildMethodTypeSignature(function: FunctionDescriptor) {
        function.extensionReceiverParameter?.let {
            buildExtensionReceiver(it, function)
        }


        val allParameters = ArrayList<ParameterDescriptor>(function.contextReceiverParameters.size + function.valueParameters.size).apply {
            addAll(function.contextReceiverParameters)
            addAll(function.valueParameters)
        }


        allParameters.collectForMangler(this, MangleConstant.VALUE_PARAMETERS) { parameter ->
            /**
             * ParameterTypes:
             *   ParameterType ( ‘;’ ParameterType  )*
             *
             * ParameterType:
             *   Type ‘...’?
             */
            buildType(parameter.type, function)
            if (parameter.isVararg) {
                append(MangleConstant.VAR_ARG_MARK)
            }
        }

        /**
         * ReturnType:
         *   '=' Type
         *   // void/unit for JVM purpose ??
         */
        append('=')
        buildType(function.returnType ?: error("No return type for $function"), function)
    }

    /**
     * ExtensionReceiverType:
     *   ‘@’ Type ‘@’
     */
    private fun StringBuilder.buildExtensionReceiver(extension: ReceiverParameterDescriptor, declaration: DeclarationDescriptor) {
        append(MangleConstant.EXTENSION_RECEIVER_MARK)
        buildType(extension.type, declaration)
        append(MangleConstant.EXTENSION_RECEIVER_MARK)
    }

    /**
     * TypeParameters:
     *   ‘{’ TypeParameter ( ‘;’ TypeParameter )* ‘}’
     */
    private fun StringBuilder.buildTypeParameters(typeParametersContainer: CallableDescriptor) {
        val selfTypeParameters = typeParametersContainer.typeParameters.filter { it.containingDeclaration === typeParametersContainer }
        if (selfTypeParameters.isEmpty()) return

        selfTypeParameters.collectForMangler(this, MangleConstant.TYPE_PARAMETERS) { parameter ->

                /**
                 * TypeParameter:
                 *   Variance? ‘<’ UpperBounds ‘>’
                 */

                buildVariance(parameter.variance)
                buildUpperBounds(parameter.upperBounds, typeParametersContainer)
            }
    }

    /**
     * Variance:
     *   ‘+’
     *   ‘-’
     */
    private fun StringBuilder.buildVariance(variance: Variance) {
        when (variance) {
            Variance.IN_VARIANCE -> append('-')
            Variance.INVARIANT -> append("")
            Variance.OUT_VARIANCE -> append('+')
        }
    }

    /**
     * UpperBounds:
     *   UpperBound ( ‘&’ UpperBound )*
     *
     * UpperBound:
     *   Type
     */
    private fun StringBuilder.buildUpperBounds(upperBounds: List<KotlinType>, declaration: DeclarationDescriptor) {
        upperBounds.map { buildString { buildType(it, declaration) } }.sorted()
            .collectForMangler(this, MangleConstant.UPPER_BOUNDS) { append(it) }
    }

    /**
     * Type:
     *   TypeConstructor TypeArguments? Nullability?
     *   ‘^d’ // stands for dynamic type
     *   ‘^e’ // stands for error type
     */
    private fun StringBuilder.buildType(wrapped: KotlinType, declaration: DeclarationDescriptor) {
        val type = wrapped.unwrap()
        when (type) {
            is DynamicType -> append(MangleConstant.DYNAMIC_TYPE_MARK)
            is ErrorType -> append(MangleConstant.ERROR_TYPE_MARK)
            is SimpleType -> {

                val classifierTypeConstructor = type.constructor as? ClassifierBasedTypeConstructor
                    ?: error("Non-denotable type constructor $type")
                buildTypeConstructor(classifierTypeConstructor, declaration)
                buildTypeArguments(classifierTypeConstructor, type.arguments, declaration)

                if (type.isMarkedNullable) {
                    append('?')
                }
            }
            is FlexibleType -> {
                val upper = type.upperBound
                val upperDescriptor = upper.constructor.declarationDescriptor ?: error("No descriptor for type $upper")
                if (upperDescriptor is ClassDescriptor) {
                    val lower = type.lowerBound
                    val lowerDescriptor = lower.constructor.declarationDescriptor as? ClassDescriptor
                        ?: error("No class descriptor for lower type $lower of $type")
                    val intermediate = if (lowerDescriptor == upperDescriptor && type !is RawType) {
                        lower.replace(newArguments = upper.arguments)
                    } else lower
                    val mixed = intermediate.makeNullableAsSpecified(upper.isMarkedNullable)
                    buildType(mixed, declaration)
                } else buildType(upper, declaration)
            }
            else -> error("Unexpected type in Signature builder $type")
        }
    }

    /**
     * TypeConstructor:
     *   ClassSignature
     *   TypeParameterRef
     */
    private fun StringBuilder.buildTypeConstructor(constructor: ClassifierBasedTypeConstructor, declaration: DeclarationDescriptor) {
        when (val descriptor = constructor.declarationDescriptor) {
            is ClassDescriptor -> buildForClass(descriptor)
            is TypeParameterDescriptor -> {
                append('{')
                append(descriptor.typeParameterIndex(declaration))
                append(':')
                append(descriptor.index)
                append('}')
            }
            else -> error("Unexpected classifier $descriptor")
        }
    }

    private fun TypeParameterDescriptor.typeParameterIndex(declaration: DeclarationDescriptor): Int {
        val tParent = containingDeclaration
        var current = if (declaration is PropertyAccessorDescriptor) { declaration.correspondingProperty } else declaration
        var i = 0
        while (declaration !is PackageFragmentDescriptor) {

            if (tParent === current) return i
            ++i
            current = current.containingDeclaration
                ?: error("Root declaration under package fragment $current")
        }

        error("No type parameter parent found for $this in hierarchy of $declaration")
    }

    /**
     * TypeArguments:
     *   ‘<’ TypeArgument ( ‘,’ TypeArgument )* ‘>’
     *
     * TypeArgument:
     *   Variance? Type
     *   ‘*’
     */
    private fun StringBuilder.buildTypeArguments(
        classifier: ClassifierBasedTypeConstructor,
        arguments: List<TypeProjection>,
        declaration: DeclarationDescriptor
    ) {
        if (arguments.isEmpty()) return
        val descriptor = classifier.declarationDescriptor

        val classDescriptor = descriptor as? ClassDescriptor ?: error("Non-empty type arguments for non-class type constructor $descriptor")

        val typeParameters = collectTypeParameters(classDescriptor)

        var index = 0

        arguments.collectForMangler(this, MangleConstant.TYPE_ARGUMENTS) { argument ->
            if (argument.isStarProjection) {
                append(MangleConstant.STAR_MARK)
            } else {
                val typeParameter = typeParameters[index]
                val effectiveVariance = effectiveVariance(typeParameter.variance, argument.projectionKind)
                buildVariance(effectiveVariance)
                buildType(argument.type, declaration)
            }
            ++index
        }
    }

    private fun collectTypeParameters(descriptor: DeclarationDescriptor): List<TypeParameterDescriptor> {
        val result = mutableListOf<TypeParameterDescriptor>()
        var tmp: DeclarationDescriptor? = descriptor
        while (tmp != null) {
            if (tmp is CallableDescriptor)
                result.addAll(tmp.typeParameters)
            else if (tmp is ClassDescriptor) {
                result.addAll(tmp.declaredTypeParameters)
            }
            tmp = tmp.containingDeclaration
        }
        return result
    }

    private fun effectiveVariance(pVariance: Variance, aVariance: Variance): Variance {
        if (pVariance == Variance.INVARIANT) return aVariance

        return pVariance
    }
}
