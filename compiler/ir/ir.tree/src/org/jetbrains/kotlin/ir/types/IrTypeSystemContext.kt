/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.FqNameEqualityChecker
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.ir.types.isPrimitiveType as irTypePredicates_isPrimitiveType

@OptIn(ObsoleteDescriptorBasedAPI::class)
interface IrTypeSystemContext : TypeSystemContext, TypeSystemCommonSuperTypesContext, TypeSystemCommonBackendContext {

    val irBuiltIns: IrBuiltIns

    override fun KotlinTypeMarker.asSimpleType() = this as? SimpleTypeMarker

    override fun KotlinTypeMarker.asFlexibleType() = this as? IrDynamicType

    override fun KotlinTypeMarker.isError() = this is IrErrorType

    override fun SimpleTypeMarker.isStubType() = false

    override fun FlexibleTypeMarker.asDynamicType() = this as? IrDynamicType

    override fun FlexibleTypeMarker.asRawType(): RawTypeMarker? = null

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is IrDynamicType)
        return irBuiltIns.anyNType as IrSimpleType
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is IrDynamicType)
        return irBuiltIns.nothingType as IrSimpleType
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? = null

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? = null

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean = (this as IrSimpleType).hasQuestionMark

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        val simpleType = this as IrSimpleType
        return if (simpleType.hasQuestionMark == nullable) simpleType
        else simpleType.run { IrSimpleTypeImpl(classifier, nullable, arguments, annotations) }
    }

    override fun SimpleTypeMarker.typeConstructor() = (this as IrSimpleType).classifier

    override fun CapturedTypeMarker.typeConstructor(): CapturedTypeConstructorMarker =
        error("Captured types should be used for IrTypes")

    override fun CapturedTypeMarker.isProjectionNotNull(): Boolean =
        error("Captured types should be used for IrTypes")

    override fun CapturedTypeMarker.captureStatus(): CaptureStatus =
        error("Captured types should be used for IrTypes")

    override fun CapturedTypeConstructorMarker.projection(): TypeArgumentMarker =
        error("Captured types should be used for IrTypes")

    override fun KotlinTypeMarker.argumentsCount(): Int =
        when (this) {
            is IrSimpleType -> arguments.size
            else -> 0
        }

    override fun KotlinTypeMarker.getArgument(index: Int): TypeArgumentMarker =
        when (this) {
            is IrSimpleType -> arguments[index]
            else -> error("Type $this has no arguments")
        }

    override fun KotlinTypeMarker.asTypeArgument() = this as IrTypeArgument

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? = error("Captured Type is not valid for IrTypes")

    override fun TypeArgumentMarker.isStarProjection() = this is IrStarProjection

    override fun TypeArgumentMarker.getVariance(): TypeVariance =
        (this as? IrTypeProjection)?.variance?.convertVariance() ?: TypeVariance.OUT

    override fun TypeArgumentMarker.getType() = (this as IrTypeProjection).type

    private fun getTypeParameters(typeConstructor: TypeConstructorMarker): List<IrTypeParameter> {
        return when (typeConstructor) {
            is IrTypeParameterSymbol -> emptyList()
            is IrClassSymbol -> extractTypeParameters(typeConstructor.owner)
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.parametersCount() = getTypeParameters(this).size

    override fun TypeConstructorMarker.getParameter(index: Int) = getTypeParameters(this)[index].symbol

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        return when (this) {
            is IrClassSymbol -> owner.superTypes
            is IrTypeParameterSymbol -> owner.superTypes
            else -> error("unsupported type constructor")
        }
    }

    override fun TypeConstructorMarker.isIntersection() = false

    override fun TypeConstructorMarker.isClassTypeConstructor() = this is IrClassSymbol

    override fun TypeParameterMarker.getVariance() = (this as IrTypeParameterSymbol).owner.variance.convertVariance()

    private fun getSuperTypes(typeParameterMarker: TypeParameterMarker) = (typeParameterMarker as IrTypeParameterSymbol).owner.superTypes

    override fun TypeParameterMarker.upperBoundCount() = getSuperTypes(this).size

    override fun TypeParameterMarker.getUpperBound(index: Int) = getSuperTypes(this)[index] as KotlinTypeMarker

    override fun TypeParameterMarker.getTypeConstructor() = this as IrTypeParameterSymbol

    override fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker) = FqNameEqualityChecker.areEqual(
        c1 as IrClassifierSymbol, c2 as IrClassifierSymbol
    )

    override fun TypeConstructorMarker.isDenotable() = true

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        val classSymbol = this as? IrClassSymbol ?: return false
        return classSymbol.owner.run { modality == Modality.FINAL && kind != ClassKind.ENUM_CLASS && kind != ClassKind.ANNOTATION_CLASS }
    }

    /*
     * fun <T> foo(x: Array<T>) {}
     * fun bar(y: Array<out CharSequence>) {
     *   foo(y)
     * }
     *
     * In this case Captured Type of `y` would be `Array<CharSequence>`
     *
     * See https://jetbrains.github.io/kotlin-spec/#type-capturing
     */

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        // TODO: is that correct?
        require(type is IrSimpleType)

        val classifier = type.classifier as? IrClassSymbol ?: return null
        val typeArguments = type.arguments

        if (!classifier.isBound) return null

        val typeParameters = extractTypeParameters(classifier.owner)

        require(typeArguments.size == typeParameters.size)

        if (typeArguments.all { it is IrTypeProjection && it.variance == Variance.INVARIANT }) return type

        val newArguments = mutableListOf<IrTypeArgument>()
        for (index in typeArguments.indices) {
            val argument = typeArguments[index]
            val parameter = typeParameters[index]

            if (argument is IrTypeProjection && argument.variance == Variance.INVARIANT) {
                newArguments += argument
                continue
            }

            val additionalBounds =
                if (argument is IrTypeProjection && argument.variance == Variance.OUT_VARIANCE) listOf(argument.type) else emptyList()

            newArguments += makeTypeProjection(
                makeTypeIntersection(parameter.superTypes + additionalBounds),
                Variance.INVARIANT
            )
        }

        return IrSimpleTypeImpl(type.classifier, type.hasQuestionMark, newArguments, type.annotations)
    }

    override fun SimpleTypeMarker.asArgumentList() = this as IrSimpleType

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean =
        this is IrClassSymbol && isClassWithFqName(StandardNames.FqNames.any)

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean =
        this is IrClassSymbol && isClassWithFqName(StandardNames.FqNames.nothing)

    override fun SimpleTypeMarker.isSingleClassifierType() = true

    override fun SimpleTypeMarker.possibleIntegerTypes() = irBuiltIns.run {
        setOf(byteType, shortType, intType, longType)
    }

    override fun TypeConstructorMarker.isIntegerLiteralTypeConstructor() = false

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound.isNothing())
        require(upperBound is IrType && upperBound.isNullableAny())
        return IrDynamicTypeImpl(null, emptyList(), Variance.INVARIANT)
    }

    // TODO: implement taking into account `isExtensionFunction`
    override fun createSimpleType(
        constructor: TypeConstructorMarker,
        arguments: List<TypeArgumentMarker>,
        nullable: Boolean,
        isExtensionFunction: Boolean
    ): SimpleTypeMarker = IrSimpleTypeImpl(constructor as IrClassifierSymbol, nullable, arguments.map { it as IrTypeArgument }, emptyList())

    private fun TypeVariance.convertVariance(): Variance {
        return when (this) {
            TypeVariance.INV -> Variance.INVARIANT
            TypeVariance.IN -> Variance.IN_VARIANCE
            TypeVariance.OUT -> Variance.OUT_VARIANCE
        }
    }

    override fun createTypeArgument(type: KotlinTypeMarker, variance: TypeVariance): TypeArgumentMarker =
        makeTypeProjection(type as IrType, variance.convertVariance())

    override fun createStarProjection(typeParameter: TypeParameterMarker) = IrStarProjectionImpl

    override fun KotlinTypeMarker.canHaveUndefinedNullability() = this is IrSimpleType && classifier is IrTypeParameterSymbol

    override fun SimpleTypeMarker.isExtensionFunction(): Boolean {
        require(this is IrSimpleType)
        return this.hasAnnotation(StandardNames.FqNames.extensionFunctionType)
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        val maxInArguments = (this as IrSimpleType).arguments.maxOfOrNull {
            if (it is IrStarProjection) 1 else it.getType().typeDepth()
        } ?: 0

        return maxInArguments + 1
    }

    override fun TypeConstructorMarker.toErrorType(): SimpleTypeMarker {
        throw IllegalStateException("Should not be called")
    }

    override fun TypeConstructorMarker.isError(): Boolean {
        throw IllegalStateException("Should not be called")
    }

    override fun findCommonIntegerLiteralTypesSuperType(explicitSupertypes: List<SimpleTypeMarker>): SimpleTypeMarker? =
        irBuiltIns.intType as IrSimpleType

    override fun KotlinTypeMarker.isNullableType(): Boolean =
        this is IrType && isNullable()

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: List<SimpleTypeMarker>): SimpleTypeMarker =
        makeTypeIntersection(types as List<IrType>) as SimpleTypeMarker

    @Suppress("UNCHECKED_CAST")
    override fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker =
        makeTypeIntersection(types as List<IrType>)

    override fun SimpleTypeMarker.isPrimitiveType(): Boolean =
        this is IrSimpleType && irTypePredicates_isPrimitiveType()

    override fun createErrorType(debugName: String): SimpleTypeMarker {
        TODO("IrTypeSystemContext doesn't support constraint system resolution")
    }

    override fun createErrorTypeWithCustomConstructor(debugName: String, constructor: TypeConstructorMarker): KotlinTypeMarker =
        TODO("IrTypeSystemContext doesn't support constraint system resolution")

    override fun nullableAnyType(): SimpleTypeMarker =
        irBuiltIns.anyNType as IrSimpleType

    override fun arrayType(componentType: KotlinTypeMarker): SimpleTypeMarker =
        irBuiltIns.arrayClass.typeWith(componentType as IrType)

    override fun KotlinTypeMarker.isArrayOrNullableArray(): Boolean =
        (this as IrType).isArray() || isNullableArray()

    override fun TypeConstructorMarker.isFinalClassOrEnumEntryOrAnnotationClassConstructor(): Boolean {
        val symbol = this as IrClassifierSymbol
        return symbol is IrClassSymbol && symbol.owner.let {
            it.modality == Modality.FINAL && !it.isEnumClass
        }
    }

    override fun KotlinTypeMarker.hasAnnotation(fqName: FqName): Boolean =
        (this as IrAnnotationContainer).hasAnnotation(fqName)

    override fun KotlinTypeMarker.getAnnotationFirstArgumentValue(fqName: FqName): Any? =
        (this as? IrType)?.annotations?.firstOrNull { annotation ->
            annotation.symbol.owner.parentAsClass.descriptor.fqNameSafe == fqName
        }?.run {
            if (valueArgumentsCount > 0) (getValueArgument(0) as? IrConst<*>)?.value else null
        }

    override fun TypeConstructorMarker.getTypeParameterClassifier(): TypeParameterMarker? =
        this as? IrTypeParameterSymbol

    override fun TypeConstructorMarker.isInlineClass(): Boolean =
        (this as? IrClassSymbol)?.owner?.isInline == true

    override fun TypeConstructorMarker.isInnerClass(): Boolean =
        (this as? IrClassSymbol)?.owner?.isInner == true

    override fun TypeParameterMarker.getRepresentativeUpperBound(): KotlinTypeMarker =
        (this as IrTypeParameterSymbol).owner.superTypes.firstOrNull {
            val irClass = it.classOrNull?.owner ?: return@firstOrNull false
            irClass.kind != ClassKind.INTERFACE && irClass.kind != ClassKind.ANNOTATION_CLASS
        } ?: owner.superTypes.first()

    override fun KotlinTypeMarker.getSubstitutedUnderlyingType(): KotlinTypeMarker? {
        // Code in inlineClassesUtils.kt loads the property with the same name from the scope of the substituted type and takes its type.
        // This code below should have the same effect.
        val irClass = (this as? IrType)?.classOrNull?.owner?.takeIf { it.isInline } ?: return null
        val inlineClassParameter = irClass.primaryConstructor?.valueParameters?.singleOrNull()
        return inlineClassParameter?.let { parameter ->
            // Taking only the type parameters of the class (and not its outer classes) is OK since inner classes are always top level
            IrTypeSubstitutor(
                irClass.typeParameters.map { it.symbol },
                (this as? IrSimpleType)?.arguments.orEmpty(),
                irBuiltIns
            ).substitute(parameter.type)
        }
    }

    override fun TypeConstructorMarker.getPrimitiveType(): PrimitiveType? {
        if (this !is IrClassSymbol || !isPublicApi) return null

        val signature = signature.asPublic()
        if (signature == null || signature.packageFqName != "kotlin") return null

        return PrimitiveType.getByShortName(signature.declarationFqName)
    }

    override fun TypeConstructorMarker.getPrimitiveArrayType(): PrimitiveType? {
        if (this !is IrClassSymbol || !isPublicApi) return null

        val signature = signature.asPublic()
        if (signature == null || signature.packageFqName != "kotlin") return null

        return PrimitiveType.getByShortArrayName(signature.declarationFqName)
    }

    override fun TypeConstructorMarker.isUnderKotlinPackage(): Boolean {
        var declaration: IrDeclaration = (this as? IrClassifierSymbol)?.owner as? IrClass ?: return false
        while (true) {
            val parent = declaration.parent
            if (parent is IrPackageFragment) {
                return parent.fqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
            }
            declaration = parent as? IrDeclaration ?: return false
        }
    }

    override fun TypeConstructorMarker.getClassFqNameUnsafe(): FqNameUnsafe? =
        (this as IrClassSymbol).owner.fqNameWhenAvailable?.toUnsafe()

    override fun TypeParameterMarker.getName(): Name =
        (this as IrTypeParameterSymbol).owner.name

    override fun TypeParameterMarker.isReified(): Boolean =
        (this as IrTypeParameterSymbol).owner.isReified

    override fun KotlinTypeMarker.isInterfaceOrAnnotationClass(): Boolean {
        val irClass = (this as IrType).classOrNull?.owner
        return irClass != null && (irClass.isInterface || irClass.isAnnotationClass)
    }
}

fun extractTypeParameters(klass: IrDeclarationParent): List<IrTypeParameter> {
    val result = mutableListOf<IrTypeParameter>()
    var current: IrDeclarationParent? = klass
    while (current != null) {
//        result += current.typeParameters
        (current as? IrTypeParametersContainer)?.let { result += it.typeParameters }
        current =
            when (current) {
                is IrField -> current.parent
                is IrClass -> when {
                    current.isInner -> current.parent as IrClass
                    current.visibility == Visibilities.LOCAL -> current.parent
                    else -> null
                }
                is IrConstructor -> current.parent as IrClass
                is IrFunction -> if (current.visibility == Visibilities.LOCAL || current.dispatchReceiverParameter != null) {
                    current.parent
                } else null
                else -> null
            }
    }
    return result
}
