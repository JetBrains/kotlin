/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.isSubstitutionOrIntersectionOverride
import org.jetbrains.kotlin.fir.scopes.getDirectOverriddenMembers
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.js.common.isES5IdentifierPart
import org.jetbrains.kotlin.js.common.isES5IdentifierStart
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import java.util.*
import kotlin.math.abs

class SuggestedName(val names: List<String>, val stable: Boolean, val declaration: FirDeclaration, val scope: FirDeclaration?)

class FirJsNameSuggestion {
    private val cache: MutableMap<FirDeclaration, SuggestedName?> = Collections.synchronizedMap(WeakHashMap())

    fun suggest(declarationWithParents: FirDeclarationWithContext<*>) =
        cache.getOrPut(declarationWithParents.declaration) { declarationWithParents.generate() }

    private fun FirDeclarationWithContext<*>.generate(): SuggestedName? {
        // Members of companion objects of classes are treated as static members of these classes
        if (isNativeObject() && declaration.isCompanion) {
            return suggest(parent!!)
        }

        // TODO: something about initialSignatureDescriptor

        // Dynamic declarations always require stable names as defined in Kotlin source code
        if (declaration.isDynamic()) {
            val name = declaration.name?.asString() ?: error("Should've had a name")
            return SuggestedName(listOf(name), true, declaration, container!!)
        }

        val containingDeclaration = container

        when {
            // TODO: ModuleDescriptor
            // TODO: PackageFragmentDescriptor
            // TODO: It's a special case when an object has `invoke` operator defined, in this case we simply generate object itself
            // TODO: TypeAliasConstructorDescriptor

            // For primary constructors and constructors of native classes we generate references to containing classes
            declaration is FirConstructor -> {
                if (declaration.isPrimary || isNativeObject()) {
                    return suggest(parent!!)
                }
            }

            // Local functions and variables are always private with their own names as suggested names
            declaration is FirCallableDeclaration ->
                if (declaration.isDescriptorWithLocalVisibility) {
                    val ownName = getNameForAnnotatedObject() ?: getSuggestedName()
                    var scope = container
                    var name = ownName

                    // Local functions always lifted to the closest class or package when they are contained inside public inline function
                    if (declaration is FirFunction) {
                        var parent = parent
                            ?: error("Should've had a parent")
                        var container = parent.declaration
                        var liftedName = ownName
                        var hasInline = false
                        while (container is FirFunction) {
                            if (container.isInline && container.ownEffectiveVisibility.isPublicAPI) {
                                hasInline = true
                            }
                            liftedName = parent.getSuggestedName() + "$" + liftedName
                            parent = parent.parent
                                ?: error("Should've had a parent")
                            container = parent.declaration
                        }
                        if (hasInline) {
                            scope = container
                            name = liftedName
                        }
                    }

                    return SuggestedName(listOf(name), false, declaration, scope)
                }
        }

        return generateDefault()
    }

    private fun FirDeclarationWithContext<*>.generateDefault(): SuggestedName {
        // For any non-local declaration suggest its own suggested name and put it in scope of its containing declaration.
        // For local declaration get a sequence for names of all containing functions and join their names with '$' symbol,
        // and use container of topmost function, i.e.
        //
        //     class A {
        //         fun foo() {
        //             fun bar() {
        //                 fun baz() { ... }
        //             }
        //         }
        //     }
        //
        // `baz` gets name 'foo$bar$baz$' scoped in `A` class.
        //
        // The exception are secondary constructors which get suggested name with '_init' suffix and are put in
        // the class's parent scope.
        //
        val parts = mutableListOf<String>()

        // For some strange reason we get FAKE_OVERRIDE for final functions called via subtype's receiver
        var current = this
        if (declaration is FirCallableDeclaration && declaration.isSubstitutionOrIntersectionOverride) {
            @Suppress("UNCHECKED_CAST")
            current as FirDeclarationWithContext<FirCallableDeclaration>
            val overridden = current.getOverridden()
            if (!overridden.isOverridableOrOverrides) {
                current = overridden
            }
        }
        val fixedDeclarationData = current

        parts += if (fixedDeclarationData.declaration is FirConstructor) {
            current = current.parent!!
            current.getSuggestedName() + "_init"
        } else {
            fixedDeclarationData.getSuggestedName()
        }

        var maybeCurrent: FirDeclarationWithContext<*>? = null

        if (current.container is FirFunction && current.declaration !is FirTypeParameter) {
            val outerFunctionName = suggest(current.parent!!)!!
            parts += outerFunctionName.names.single()
            maybeCurrent = outerFunctionName.scope?.let { FirDeclarationWithContext(it, context) }
        } else {
            maybeCurrent = current.parent
        }

        // Getters and setters have generation strategy similar to common declarations, except for they are declared as
        // members of classes/packages, not corresponding properties.
        if (maybeCurrent?.declaration is FirProperty) {
            maybeCurrent = maybeCurrent.parent
        }

        parts.reverse()
        val unmangledName = parts.joinToString("$")
        val (id, stable) = fixedDeclarationData.mangleNameIfNecessary(unmangledName)
        return SuggestedName(listOf(id), stable, fixedDeclarationData.declaration, maybeCurrent?.declaration)
    }
}

private val PROPERTIES_TYPES = setOf(
    StandardClassIds.KProperty0,
    StandardClassIds.KProperty1,
    StandardClassIds.KProperty2,
    StandardClassIds.KMutableProperty0,
    StandardClassIds.KMutableProperty1,
    StandardClassIds.KMutableProperty2,
)

private fun isNumberedKPropertyOrKMutablePropertyType(classId: ClassId): Boolean {
    return classId in PROPERTIES_TYPES
}

private fun FirDeclaration.isDynamic(): Boolean {
    if (this !is FirCallableDeclaration) return false
    val dispatchReceiverParameter = dispatchReceiverType
    return dispatchReceiverParameter != null && dispatchReceiverParameter.type is ConeDynamicType
}

private val FirDeclaration.isDescriptorWithLocalVisibility
    get() = this is FirMemberDeclaration && this.status.visibility == Visibilities.Local

private fun FirDeclaration.getAnnotationStringParameter(classId: ClassId): String? {
    val annotation = getAnnotationByClassId(classId) as? FirAnnotationCall
    val expression = annotation?.arguments?.firstOrNull() as? FirConstExpression<*>
    return expression?.value as? String
}

fun FirDeclaration.getJsName(): String? {
    return getAnnotationStringParameter(StandardClassIds.Annotations.JsName)
}

val FirDeclaration.name
    get() = when (this) {
        is FirVariable -> name
        is FirSimpleFunction -> name
        is FirRegularClass -> name
        else -> null
    }

val WITH_CUSTOM_NAME_ANNOTATIONS = setOf(StandardClassIds.Annotations.JsLibrary, StandardClassIds.Annotations.JsNative)

private fun FirDeclarationWithContext<*>.getNameForAnnotatedObject(): String? {
    val defaultJsName = declaration.getJsName()

    for (annotation in WITH_CUSTOM_NAME_ANNOTATIONS) {
        if (!hasAnnotationOrInsideAnnotatedClass(annotation)) {
            continue
        }

        var name = getNameForAnnotatedObject(declaration, annotation)

        if (name == null) {
            name = defaultJsName
        }

        return name ?: declaration.name?.asString()
    }

    if (defaultJsName != null && (isEffectivelyExternal() || isExportedObject())) {
        return declaration.name?.asString()
    }

    return defaultJsName
}

private inline fun <reified T> FirDeclarationWithContext<*>.findParent(): T? {
    var current: FirDeclarationWithContext<*>? = this

    while (current != null && current.declaration !is T) {
        current = current.parent
    }

    return current?.declaration as? T
}

fun FirDeclarationWithContext<*>.isExportedObject(): Boolean {
    if (declaration is FirMemberDeclaration) {
        if (declaration.visibility != Visibilities.Public) return false
    }

    if (hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.JsExport)) return true

    val file = findParent<FirFile>()
    return file?.hasAnnotation(StandardClassIds.Annotations.JsExport) == true
}

fun getNameForAnnotatedObject(declaration: FirDeclaration, annotation: ClassId): String? {
    return declaration.getAnnotationStringParameter(annotation)
}

private val FirDeclaration.isCompanion get() = this is FirMemberDeclaration && status.isCompanion

@OptIn(SymbolInternals::class)
fun FirDeclarationWithContext<*>.isNativeObject(): Boolean {
    if (hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.JsNative) || isEffectivelyExternal()) {
        return true
    }

    if (declaration is FirPropertyAccessor) {
        val property = declaration.propertySymbol?.fir ?: error("Should've had a property")
        return withSameParents(property).hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.JsNative)
    }

    return if (declaration is FirAnonymousInitializer) {
        parent?.isNativeObject() == true
    } else {
        false
    }
}

fun FirDeclarationWithContext<*>.isLibraryObject() = hasAnnotationOrInsideAnnotatedClass(StandardClassIds.Annotations.JsLibrary)

fun FirDeclarationWithContext<*>.isEffectivelyExternalMember(): Boolean {
    return declaration is FirMemberDeclaration && isEffectivelyExternal()
}

val PREDEFINED_ANNOTATIONS = setOf(
    StandardClassIds.Annotations.JsLibrary,
    StandardClassIds.Annotations.JsNative,
    StandardClassIds.Annotations.JsNativeInvoke,
    StandardClassIds.Annotations.JsNativeGetter,
    StandardClassIds.Annotations.JsNativeSetter,
)

fun FirDeclarationWithContext<*>.isPredefinedObject(): Boolean {
    if (declaration is FirMemberDeclaration && (declaration.isExpect)) return true
    if (isEffectivelyExternalMember()) return true

    for (annotation in PREDEFINED_ANNOTATIONS) {
        if (hasAnnotationOrInsideAnnotatedClass(annotation)) {
            return true
        }
    }

    return false
}

fun FirDeclarationWithContext<*>.hasAnnotationOrInsideAnnotatedClass(classId: ClassId): Boolean {
    if (declaration.hasAnnotation(classId)) return true
    return parent?.hasAnnotationOrInsideAnnotatedClass(classId) == true
}

@OptIn(SymbolInternals::class)
fun FirDeclarationWithContext<*>.isEffectivelyExternal(): Boolean {
    if (declaration is FirMemberDeclaration && declaration.isExternal) return true

    if (declaration is FirPropertyAccessor) {
        val property = declaration.propertySymbol?.fir ?: error("Should've had a property")
        if (withSameParents(property).isEffectivelyExternal()) return true
    }

    if (declaration is FirProperty) {
        if (declaration.getter?.isExternal == true && (!declaration.isVar || declaration.setter?.isExternal == true)) {
            return true
        }
    }

    return parent?.isEffectivelyExternal() == true
}

// For regular names suggest its string representation
// For property accessors suggest name of a property with 'get_' and 'set_' prefixes
// For anonymous declarations (i.e. lambdas and object expressions) suggest 'f'
private fun FirDeclarationWithContext<*>.getSuggestedName(): String {
    val name = declaration.name
    return when {
        name != null && !name.isSpecial -> name.asString()
        declaration is FirPropertyAccessor -> {
            val property = declaration.propertySymbol?.fir ?: error("Should've had a property")
            if (declaration.isGetter) {
                "get_" + withSameParents(property).getSuggestedName()
            } else {
                "set_" + withSameParents(property).getSuggestedName()
            }
        }
        else -> "f"
    }
}

private fun FirDeclarationWithContext<*>.mangleNameIfNecessary(baseName: String): NameAndStability {
    // If we have a callable declaration (property or method) it can override method in a parent class.
    // Traverse to the topmost overridden method.
    // It does not matter which path to choose during traversal, since front-end must ensure
    // that names required by different overridden method do no differ.
    val overriddenDeclaration = if (declaration is FirCallableDeclaration) {
        @Suppress("UNCHECKED_CAST")
        this as FirDeclarationWithContext<FirCallableDeclaration>
        getOverridden()
    } else {
        this
    }

    // If declaration is marked with either external, @native, @library or @JsName, return its stable name as is.
    val nativeName = overriddenDeclaration.getNameForAnnotatedObject()
    if (nativeName != null) return NameAndStability(nativeName, true)

    if (overriddenDeclaration.declaration is FirFunction) {
        when (overriddenDeclaration.declaration.symbol.callableId.asSingleFqName().asString()) {
            "kotlin.CharSequence.get" -> return NameAndStability("charCodeAt", true)
            "kotlin.Any.equals" -> return NameAndStability("equals", true)
        }
        val container = overriddenDeclaration.container
        if (container is FirClass && isNumberedKPropertyOrKMutablePropertyType(container.classId)) {
            when (val name = overriddenDeclaration.declaration.name?.asString() ?: error("Should've had a name")) {
                "get", "set" -> return NameAndStability(name, true)
            }
        }
    } else if (overriddenDeclaration.declaration is FirProperty) {
        when (overriddenDeclaration.declaration.symbol.callableId.asSingleFqName().asString()) {
            "kotlin.reflect.KCallable.name" -> return NameAndStability("callableName", true)
        }
    }

    return overriddenDeclaration.mangleRegularNameIfNecessary(baseName)
}

private fun FirDeclarationWithContext<FirCallableDeclaration>.getOverridden(): FirDeclarationWithContext<FirCallableDeclaration> {
    val overridden = generateSequence(declaration) { it.getAllOverridden(session, context).firstOrNull()?.fir }.last()

    return if (overridden == declaration) {
        this
    } else {
        FirDeclarationWithContext(overridden, context)
    }
}

fun FirCallableDeclaration.getAllOverridden(
    session: FirSession,
    context: CheckerContext,
): List<FirCallableSymbol<out FirCallableDeclaration>> {
    val classSymbol = getContainingClassSymbol(session) as? FirClassSymbol ?: return emptyList()
    val scope = classSymbol.unsubstitutedScope(context)

    scope.processFunctionsByName(symbol.name) { }
    return scope.getDirectOverriddenMembers(symbol, true)
}

private val FirDeclaration.isDeclarationWithLocalVisibility
    get() = this is FirMemberDeclaration && visibility == Visibilities.Local

private fun FirFunction.isEnumValueOfMethod(session: FirSession): Boolean {
    val methodTypeParameters = valueParameters
    val nullableString = session.builtinTypes.nullableStringType.type
    return StandardNames.ENUM_VALUE_OF == name
            && methodTypeParameters.size == 1
            && methodTypeParameters[0].returnTypeRef.coneType.isSubtypeOf(nullableString, session)
}

private val FirClass.isFinalClass
    get() = isFinal && classKind != ClassKind.ENUM_CLASS

private val FirDeclarationWithContext<*>.isOverridable
    get() = declaration is FirCallableDeclaration
            && declaration.visibility != Visibilities.Private
            && declaration.modality != Modality.FINAL
            && (container as? FirClass)?.isFinalClass != true

private val FirDeclarationWithContext<FirCallableDeclaration>.isOverridableOrOverrides
    get() = isOverridable || declaration.isOverride

private fun FirDeclarationWithContext<*>.mangleRegularNameIfNecessary(baseName: String): NameAndStability {
    if (declaration is FirClassLikeDeclaration) {
        return NameAndStability(baseName, !declaration.isDeclarationWithLocalVisibility)
    }

    fun regularAndUnstable() = NameAndStability(baseName, false)

    if (declaration !is FirCallableDeclaration) {
        // Actually, only reified types get here, and it would be properly to put assertion here
        // However, it's better to generate wrong code than crash
        return regularAndUnstable()
    }

    @Suppress("UNCHECKED_CAST")
    this as? FirDeclarationWithContext<FirCallableDeclaration> ?: error("Guaranteed by the if above")

    fun mangledAndStable() = NameAndStability(getStableMangledName(baseName, encodeSignature()), true)
    fun mangledInternal() = NameAndStability(getInternalMangledName(baseName, encodeSignature()), true)
    fun mangledPrivate() = NameAndStability(getPrivateMangledName(baseName), false)

    val effectiveVisibility = declaration.ownEffectiveVisibility

    return when (val containingDeclaration = container) {
        null, is FirFile -> when {
            effectiveVisibility.isPublicAPI -> mangledAndStable()

            effectiveVisibility == Visibilities.Internal -> mangledInternal()

            else -> regularAndUnstable()
        }
        is FirClass -> when {
            // valueOf() is created in the library with a mangled name for every enum class
            declaration is FirFunction && declaration.isEnumValueOfMethod(session) -> mangledAndStable()

            // Make all public declarations stable
            effectiveVisibility == Visibilities.Public -> mangledAndStable()

            isOverridableOrOverrides -> mangledAndStable()

            // Make all protected declarations of non-final public classes stable
            effectiveVisibility == Visibilities.Protected
                    && !containingDeclaration.isFinalClass
                    && containingDeclaration.visibility.isPublicAPI -> mangledAndStable()

            effectiveVisibility == Visibilities.Internal -> mangledInternal()

            // Mangle (but make unstable) all non-public API of public classes
            containingDeclaration.visibility.isPublicAPI && !containingDeclaration.isFinalClass -> mangledPrivate()

            else -> regularAndUnstable()
        }
        else -> {
            assert(containingDeclaration is FirCallableDeclaration) {
                "containingDeclaration for declaration have unsupported type for mangling, " +
                        "declaration: " + declaration + ", containingDeclaration: " + containingDeclaration
            }
            regularAndUnstable()
        }
    }
}

data class NameAndStability(val name: String, val stable: Boolean)

private fun FirDeclarationWithContext<FirCallableDeclaration>.getPrivateMangledName(baseName: String): String {
    val ownerName = container?.singleFqName?.asString() ?: ""

    // Base name presents here since name part gets sanitized, so we have to produce different suffixes to distinguish
    // between, say `.` and `;`.
    return getStableMangledName(
        sanitizeName(baseName),
        ownerName + "." + baseName + ":" + encodeSignature(),
    )
}

private fun getInternalMangledName(suggestedName: String, forCalculateId: String): String {
    val suffix = "_${mangledId("internal:$forCalculateId")}\$"
    return suggestedName + suffix
}

private fun getStableMangledName(suggestedName: String, forCalculateId: String): String {
    val suffix = if (forCalculateId.isEmpty()) "" else "_${mangledId(forCalculateId)}\$"
    return suggestedName + suffix
}

private fun mangledId(forCalculateId: String): String {
    val absHashCode = abs(forCalculateId.hashCode())
    return if (absHashCode != 0) absHashCode.toString(Character.MAX_RADIX) else ""
}

private val FirMemberDeclaration.ownEffectiveVisibility: Visibility
    get() = visibility.toEffectiveVisibility(null, this is FirClass, checkPublishedApi = true).toVisibility()

fun sanitizeName(name: String): String {
    if (name.isEmpty()) return "_"

    val first = name.first().let { if (it.isES5IdentifierStart()) it else '_' }
    return first.toString() + name.drop(1).map { if (it.isES5IdentifierPart()) it else '_' }.joinToString("")
}

fun <T> List<T>.withoutLast() = subList(0, size - 1)
