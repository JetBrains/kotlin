/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.methods

import com.intellij.navigation.ItemPresentation
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightIdentifier
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.psiSafe
import org.jetbrains.kotlin.analysis.api.symbols.sourcePsiSafe
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.analysis.api.types.KaTypePointer
import org.jetbrains.kotlin.asJava.builder.LightMemberOrigin
import org.jetbrains.kotlin.asJava.classes.METHOD_INDEX_BASE
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.annotations.SymbolLightSimpleAnnotation
import org.jetbrains.kotlin.light.classes.symbol.cachedValue
import org.jetbrains.kotlin.light.classes.symbol.classes.SymbolLightClassForClassOrObject
import org.jetbrains.kotlin.light.classes.symbol.compareSymbolPointers
import org.jetbrains.kotlin.light.classes.symbol.isValid
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightTypeParameterList
import org.jetbrains.kotlin.light.classes.symbol.parameters.SymbolLightValueParameter
import org.jetbrains.kotlin.light.classes.symbol.withSymbol
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration

/**
 * Alternative implementation of SymbolLightMethodForMappedClass that uses KaSymbol abstractions
 * instead of PsiMethod and PsiSubstitutor.
 *
 * This version stores Kotlin type information using KaType and performs type substitution and
 * conversion to PsiType lazily, aligning with the architecture of other Symbol light classes.
 *
 * Key differences from the original implementation:
 * - Uses KaSymbolPointer<KaNamedFunctionSymbol> instead of PsiMethod
 * - Uses KaType for type substitution instead of PsiSubstitutor
 * - Parameter and return type computation uses asPsiType() from KaType
 * - Follows the pattern established by SymbolLightMethod and SymbolLightSimpleMethod
 */
internal class SymbolLightMethodForMappedClassV2 private constructor(
    private val functionSymbolPointer: KaSymbolPointer<KaNamedFunctionSymbol>,
    lightMemberOrigin: LightMemberOrigin?,
    containingClass: SymbolLightClassForClassOrObject,
    private val functionDeclaration: KtCallableDeclaration?,
    override val kotlinOrigin: KtDeclaration?,
    private val methodName: String,
    private val isFinal: Boolean,
    private val hasImplementation: Boolean,
    /**
     * Substitution map from Java type parameters to Kotlin type arguments.
     * This is used when mapping generic Java collection methods to concrete Kotlin types.
     * For example, when MyList<String> implements java.util.List<E>, we map E -> String.
     * The map stores pointers to both type parameters and types for safe cross-session access.
     */
    private val substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>>,
    /**
     * Explicitly provided signature that overrides the computed signature from the function symbol.
     * Used for special cases like Map methods (get(K), remove(K)) where parameter and return types
     * need custom handling.
     */
    private val providedSignature: MappedMethodSignature?,
    /**
     * Type to substitute for java.lang.Object in the signature.
     * Used when mapping Java methods that use Object to Kotlin types with more specific bounds.
     */
    private val substituteObjectWith: KaTypePointer<KaType>?,
) : SymbolLightMethodBase(
    lightMemberOrigin = lightMemberOrigin,
    containingClass = containingClass,
    methodIndex = METHOD_INDEX_BASE,
    isJvmExposedBoxed = false
) {

    constructor(
        functionSymbol: KaNamedFunctionSymbol,
        lightMemberOrigin: LightMemberOrigin?,
        containingClass: SymbolLightClassForClassOrObject,
        name: String,
        isFinal: Boolean,
        hasImplementation: Boolean,
        substitutionMap: Map<KaSymbolPointer<KaTypeParameterSymbol>, KaTypePointer<KaType>> = emptyMap(),
        providedSignature: MappedMethodSignature? = null,
        substituteObjectWith: KaTypePointer<KaType>? = null,
    ) : this(
        functionSymbolPointer = functionSymbol.createPointer(),
        lightMemberOrigin = lightMemberOrigin,
        containingClass = containingClass,
        functionDeclaration = functionSymbol.sourcePsiSafe(),
        kotlinOrigin = functionSymbol.sourcePsiSafe() ?: lightMemberOrigin?.originalElement ?: functionSymbol.psiSafe<KtDeclaration>(),
        methodName = name,
        isFinal = isFinal,
        hasImplementation = hasImplementation,
        substitutionMap = substitutionMap,
        providedSignature = providedSignature,
        substituteObjectWith = substituteObjectWith,
    )

    init {
        if (!hasImplementation && isFinal) {
            error("Can't be final without an implementation")
        }
    }

    private inline fun <T> withFunctionSymbol(crossinline action: KaSession.(KaNamedFunctionSymbol) -> T): T =
        functionSymbolPointer.withSymbol(ktModule, action)

    override fun getPresentation(): ItemPresentation? =
        kotlinOrigin?.presentation

    override fun getNavigationElement(): PsiElement =
        kotlinOrigin?.navigationElement ?: super.getNavigationElement()

    override fun getIcon(flags: Int) =
        kotlinOrigin?.getIcon(flags)

    override fun hasModifierProperty(name: String): Boolean = when (name) {
        PsiModifier.ABSTRACT -> !hasImplementation
        PsiModifier.FINAL -> isFinal
        PsiModifier.PUBLIC -> true
        else -> false
    }

    override fun getParameterList(): PsiParameterList = cachedValue {
        SymbolLightParameterList(
            parent = this@SymbolLightMethodForMappedClassV2,
            correspondingCallablePointer = functionSymbolPointer,
        ) { builder ->
            withFunctionSymbol { functionSymbol ->
                functionSymbol.valueParameters.forEachIndexed { index, parameter ->
                    builder.addParameter(
                        createParameter(
                            parameter = parameter,
                            index = index,
                        )
                    )
                }
            }
        }
    }

    private fun KaSession.createParameter(
        parameter: KaValueParameterSymbol,
        index: Int,
    ): PsiParameter {
        val paramType = computeParameterType(parameter, index)
        return SymbolLightValueParameterWithProvidedType(
            parameterSymbol = parameter,
            containingMethod = this@SymbolLightMethodForMappedClassV2,
            providedType = paramType,
        )
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.computeParameterType(parameter: KaValueParameterSymbol, index: Int): PsiType {
        val providedType = providedSignature?.parameterTypes?.getOrNull(index)?.restore()
        val type = providedType ?: applySubstitution(parameter.returnType)
        return type.asPsiType(
            useSitePosition = this@SymbolLightMethodForMappedClassV2,
            allowErrorTypes = true,
            mode = KaTypeMappingMode.VALUE_PARAMETER,
        ) ?: PsiTypes.nullType()
    }

    @OptIn(KaExperimentalApi::class)
    private fun KaSession.applySubstitution(type: KaType): KaType {
        if (substitutionMap.isEmpty()) {
            return type
        }

        // Build a KaSubstitutor from the substitution map
        val substitutor = buildSubstitutor {
            substitutionMap.forEach { (typeParameterPointer, typeArgumentPointer) ->
                val typeParameter = typeParameterPointer.restoreSymbol() ?: return@forEach
                val typeArgument = typeArgumentPointer.restore() ?: return@forEach
                substitution(typeParameter, typeArgument)
            }
        }

        val substituted = substitutor.substitute(type)
        val isAnyType = substituted.isAnyType || (substituted as? KaFlexibleType)?.lowerBound?.isAnyType == true

        return if (isAnyType && substituteObjectWith != null) {
            substituteObjectWith.restore() ?: substituted
        } else {
            substituted
        }
    }

    override fun getName(): String = methodName

    @OptIn(KaExperimentalApi::class)
    override fun getReturnType(): PsiType? = cachedValue {
        withFunctionSymbol { functionSymbol ->
            val providedType = providedSignature?.returnType?.restore()
            val type = providedType ?: applySubstitution(functionSymbol.returnType)
            type.asPsiType(
                useSitePosition = this@SymbolLightMethodForMappedClassV2,
                allowErrorTypes = true,
                mode = KaTypeMappingMode.FUNCTION_RETURN_TYPE,
            )
        }
    }

    private val _typeParameterList: PsiTypeParameterList? by lazyPub {
        if (!hasTypeParameters()) return@lazyPub null

        SymbolLightTypeParameterList(
            owner = this,
            symbolWithTypeParameterPointer = functionSymbolPointer,
            ktModule = ktModule,
            ktDeclaration = functionDeclaration,
        )
    }

    override fun getTypeParameters(): Array<PsiTypeParameter> =
        _typeParameterList?.typeParameters ?: PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList(): PsiTypeParameterList? = _typeParameterList

    override fun hasTypeParameters(): Boolean = withFunctionSymbol { it.typeParameters.isNotEmpty() }

    override fun isOverride(): Boolean = true

    override fun isVarArgs(): Boolean = withFunctionSymbol { functionSymbol ->
        functionSymbol.valueParameters.lastOrNull()?.isVararg == true
    }

    override fun isConstructor(): Boolean = false

    private val identifier: LightIdentifier by lazyPub { LightIdentifier(manager, methodName) }

    override fun getNameIdentifier(): LightIdentifier = identifier

    override fun getDocComment(): PsiDocComment? = null

    private val _modifierList: PsiModifierList by lazyPub {
        object : LightModifierList(manager, language) {
            override fun getParent(): PsiElement = this@SymbolLightMethodForMappedClassV2

            private val overrideAnnotation by lazy {
                SymbolLightSimpleAnnotation(fqName = CommonClassNames.JAVA_LANG_OVERRIDE, parent = this)
            }

            private val allAnnotations: Array<PsiAnnotation> by lazy { arrayOf(overrideAnnotation) }

            override fun hasModifierProperty(name: String): Boolean =
                this@SymbolLightMethodForMappedClassV2.hasModifierProperty(name)

            override fun hasExplicitModifier(name: String): Boolean = hasModifierProperty(name)

            override fun getAnnotations(): Array<PsiAnnotation> = allAnnotations

            override fun findAnnotation(qualifiedName: String): PsiAnnotation? =
                if (qualifiedName == CommonClassNames.JAVA_LANG_OVERRIDE) overrideAnnotation else null

            override fun hasAnnotation(qualifiedName: String): Boolean =
                qualifiedName == CommonClassNames.JAVA_LANG_OVERRIDE
        }
    }

    override fun getModifierList(): PsiModifierList = _modifierList

    override fun isDeprecated(): Boolean = false

    override fun isValid(): Boolean =
        super.isValid() && (functionDeclaration?.isValid ?: functionSymbolPointer.isValid(ktModule))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SymbolLightMethodForMappedClassV2) return false

        if (methodName != other.methodName) return false
        if (isFinal != other.isFinal) return false
        if (hasImplementation != other.hasImplementation) return false
        if (providedSignature != other.providedSignature) return false
        if (containingClass != other.containingClass) return false
        if (ktModule != other.ktModule) return false

        if (functionDeclaration != null || other.functionDeclaration != null) {
            return functionDeclaration == other.functionDeclaration
        }

        return compareSymbolPointers(functionSymbolPointer, other.functionSymbolPointer)
    }

    override fun hashCode(): Int {
        var result = methodName.hashCode()
        result = 31 * result + isFinal.hashCode()
        result = 31 * result + hasImplementation.hashCode()
        result = 31 * result + (providedSignature?.hashCode() ?: 0)
        result = 31 * result + containingClass.hashCode()
        result = 31 * result + (functionDeclaration?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName.orEmpty()}:$methodName"
}

/**
 * Method signature that uses pointers to KaType instead of PsiType for parameter and return types.
 * PsiTypes are computed lazily from KaTypes using the provided context.
 * Pointers will be unwrapped on the use-site.
 */
internal data class MappedMethodSignature(
    val parameterTypes: List<KaTypePointer<KaType>>,
    val returnType: KaTypePointer<KaType>,
)

/**
 * Custom implementation of SymbolLightValueParameter that uses a provided PsiType
 * instead of computing it from the parameter symbol.
 * This is necessary for mapped class methods where the parameter type needs to be
 * customized (e.g., with type substitution or from a provided signature).
 */
private class SymbolLightValueParameterWithProvidedType(
    parameterSymbol: KaValueParameterSymbol,
    containingMethod: SymbolLightMethodBase,
    private val providedType: PsiType,
) : SymbolLightValueParameter(parameterSymbol, containingMethod) {
    override fun getType(): PsiType = providedType
}
