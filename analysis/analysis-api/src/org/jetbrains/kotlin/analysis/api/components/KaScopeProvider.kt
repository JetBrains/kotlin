/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import java.util.*

public interface KaScopeProvider : KaSessionComponent {
    /**
     * A [KaScope] containing *non-static* callable members (functions, properties, and constructors) and all classifier members
     * (classes and objects) of the given [KaDeclarationContainerSymbol]. The scope includes members inherited from the symbol's supertypes,
     * in addition to members which are declared explicitly inside the symbol's body.
     *
     * The member scope doesn't include [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters). For
     * a scope which contains synthetic properties, please refer to [syntheticJavaPropertiesScope].
     *
     * @see staticMemberScope
     */
    public val KaDeclarationContainerSymbol.memberScope: KaScope

    /**
     * A [KaScope] containing the *static* members of the given [KaDeclarationContainerSymbol].
     *
     * The behavior of the scope differs based on whether the given [KaDeclarationContainerSymbol] is a Kotlin or Java class:
     *
     * - **Kotlin class:** The scope contains static callables (functions and properties) and classifiers (classes and objects) declared
     *   directly in the [KaDeclarationContainerSymbol]. Hence, the static member scope for Kotlin classes is equivalent to
     *   [staticDeclaredMemberScope].
     * - **Java class:** The scope contains static callables (functions and properties) declared in the [KaDeclarationContainerSymbol] or
     *   any of its superclasses (excluding static callables from super-interfaces), and classes declared directly in the
     *   [KaDeclarationContainerSymbol]. This follows Kotlin's rules about static inheritance in Java classes, where static callables are
     *   propagated from superclasses, but nested classes are not.
     *
     * #### Kotlin Example
     *
     * ```kotlin
     * abstract class A {
     *     class C1
     *     inner class D1
     *     object O1
     *
     *     // There is no way to declare a static callable in an abstract class, as only enum classes define additional static callables.
     * }
     *
     * class B : A() {
     *     class C2
     *     inner class D2
     *     object O2
     *     companion object {
     *         val baz: String = ""
     *     }
     * }
     * ```
     *
     * The static member scope of `B` contains the following symbols:
     *
     * ```
     * class C2
     * inner class D2
     * object O2
     * companion object
     * ```
     *
     * #### Java Example
     *
     * ```java
     * // SuperInterface.java
     * public interface SuperInterface {
     *     public static void fromSuperInterface() { }
     * }
     *
     * // SuperClass.java
     * public abstract class SuperClass implements SuperInterface {
     *     static class NestedSuperClass { }
     *     class InnerSuperClass { }
     *     public static void fromSuperClass() { }
     * }
     *
     * // FILE: JavaClass.java
     * public class JavaClass extends SuperClass {
     *     static class NestedClass { }
     *     class InnerClass { }
     *     public static void fromJavaClass() { }
     * }
     * ```
     *
     * The static member scope of `JavaClass` contains the following symbols:
     *
     * ```
     * public static void fromSuperClass()
     * public static void fromJavaClass()
     * static class NestedClass
     * class InnerClass
     * ```
     *
     * @see memberScope
     */
    public val KaDeclarationContainerSymbol.staticMemberScope: KaScope

    /**
     * A [KaScope] containing *all* members from [memberScope] and [staticMemberScope].
     */
    public val KaDeclarationContainerSymbol.combinedMemberScope: KaScope
        get() = withValidityAssertion {
            return listOf(memberScope, staticMemberScope).asCompositeScope()
        }

    /**
     * A [KaScope] containing the *non-static* callables (functions, properties, and constructors) and inner classes explicitly
     * declared in the given [KaDeclarationContainerSymbol].
     *
     * The declared member scope does not contain classifiers (including the companion object) except for inner classes. To retrieve the
     * classifiers declared in this [KaDeclarationContainerSymbol], please use the *static* declared member scope provided by
     * [staticDeclaredMemberScope].
     *
     * @see staticDeclaredMemberScope
     */
    public val KaDeclarationContainerSymbol.declaredMemberScope: KaScope

    /**
     * A [KaScope] containing the *static* callables (functions and properties) and all classifiers (classes and objects) explicitly
     * declared in the given [KaDeclarationContainerSymbol].
     *
     * It is worth noting that, while Java classes may contain declarations of static callables freely, in Kotlin only enum classes define
     * static callables. Hence, for non-enum Kotlin classes, it is not expected that the static declared member scope will contain any
     * callables.
     *
     * @see declaredMemberScope
     */
    public val KaDeclarationContainerSymbol.staticDeclaredMemberScope: KaScope

    /**
     * A [KaScope] containing *all* members explicitly declared in the given [KaDeclarationContainerSymbol].
     *
     * In contrast to [declaredMemberScope] and [staticDeclaredMemberScope], this scope contains both static and non-static members.
     */
    public val KaDeclarationContainerSymbol.combinedDeclaredMemberScope: KaScope

    /**
     * A [KaScope] containing synthetic callables (functions and properties) created by interface delegation.
     *
     * #### Example
     *
     * ```kotlin
     * interface I {
     *     val foo: Int get() = 2
     *     fun bar(): String
     * }
     *
     * class A(
     *     private val p: I
     * ) : I by p {
     *     val regularProperty: Int = 5
     * }
     * ```
     *
     * The delegated member scope for `A` has the following entries:
     *
     * ```
     * override val foo: kotlin.Int
     *   get()
     *
     * override fun bar(): kotlin.String
     * ```
     *
     * `regularProperty` is not contained in the delegated member scope because it is not a delegated property.
     */
    public val KaDeclarationContainerSymbol.delegatedMemberScope: KaScope

    /**
     * A [KaScope] containing the top-level declarations (such as classes, functions and properties) in the given [KaFileSymbol].
     */
    public val KaFileSymbol.fileScope: KaScope

    /**
     * A [KaScope] containing all members of the package represented by the given [KaPackageSymbol], not including members of subpackages.
     */
    public val KaPackageSymbol.packageScope: KaScope

    /**
     * Combines a list of [KaScope]s into a single composite [KaScope]. The resulting scope contains all members of its constituent scopes.
     */
    public fun List<KaScope>.asCompositeScope(): KaScope

    /**
     * A [KaTypeScope] for the given [KaType], or `null` if the type is [erroneous][org.jetbrains.kotlin.analysis.api.types.KaErrorType].
     * The scope includes all members which are callable on a given type. It also includes [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters).
     *
     * Comparing to [KaScope], the [KaTypeScope] contains members whose use-site type parameters have been substituted.
     *
     * #### Example
     *
     * ```kotlin
     * fun foo(list: List<String>) {
     *     list
     * }
     *```
     *
     * We can get a [KaTypeScope] for the [expression type][org.jetbrains.kotlin.analysis.api.components.KaExpressionTypeProvider.expressionType]
     * of `list`. This scope contains a `get(index: Int): String` function, where the return type `E` from [List.get] is substituted with
     * the type argument `String`.
     *
     * @see KaTypeScope
     * @see KaTypeProvider.type
     * @see KaExpressionTypeProvider.expressionType
     */
    @KaExperimentalApi
    public val KaType.scope: KaTypeScope?

    /**
     * A [KaScope] containing unsubstituted declarations from the [KaType]'s underlying declaration.
     */
    @KaExperimentalApi
    public val KaTypeScope.declarationScope: KaScope

    /**
     * A [KaTypeScope] containing the [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters) created
     * for a given [KaType].
     */
    @KaExperimentalApi
    public val KaType.syntheticJavaPropertiesScope: KaTypeScope?

    /**
     * Computes the lexical scope context for a given [position] in the [KtFile]. The scope context includes all scopes that are relevant
     * for the given position, together with all available implicit receivers.
     */
    public fun KtFile.scopeContext(position: KtElement): KaScopeContext

    /**
     * A [KaScopeContext] formed from all imports in the [KtFile].
     *
     * By default, the scope context also includes default importing scopes, which can be filtered by [KaScopeKind].
     */
    public val KtFile.importingScopeContext: KaScopeContext

    /**
     * Returns a single [KaScope] that contains declarations from all scopes that satisfy [filter].
     *
     * The order of declarations corresponds to the order of their containing scopes, which are sorted according to their [indices][KaScopeKind.indexInTower]
     * in the scope tower.
     */
    public fun KaScopeContext.compositeScope(filter: (KaScopeKind) -> Boolean = { true }): KaScope = withValidityAssertion {
        val subScopes = scopes.filter { filter(it.kind) }.map { it.scope }
        subScopes.asCompositeScope()
    }
}

/**
 * A scope context includes all scopes that are relevant for a given [KtElement] position in a [KtFile], together with all available
 * implicit receivers.
 *
 * @see KaScopeProvider.scopeContext
 */
public interface KaScopeContext : KaLifetimeOwner {
    /**
     * The implicit receivers available at the context position.
     *
     * The list is sorted according to the order of scopes in the scope tower (from innermost to outermost).
     */
    @OptIn(KaExperimentalApi::class)
    public val implicitReceivers: List<KaImplicitReceiver>
        get() = implicitValues.filterIsInstance<KaImplicitReceiver>()

    /**
     * The implicit values available at the context position.
     *
     * The list is sorted according to the order of scopes in the scope tower (from innermost to outermost).
     *
     * @see KaScopeKind.indexInTower
     */
    @KaExperimentalApi
    public val implicitValues: List<KaScopeImplicitValue>

    /**
     * The [KaScope]s available at the context position. [KaScopeWithKind] additionally determines the kind of scope at the index in the
     * scope tower.
     *
     * The list is sorted according to the order of scopes in the scope tower (from innermost to outermost).
     */
    public val scopes: List<KaScopeWithKind>
}

/**
 * Represents a value which can be used implicitly inside a particular [KaScopeContext].
 */
@KaExperimentalApi
public sealed interface KaScopeImplicitValue : KaLifetimeOwner {
    /**
     * The implicit value type.
     */
    public val type: KaType

    /**
     * The index of the scope in the scope tower where the implicit value is declared.
     */
    public val scopeIndexInTower: Int
}

/**
 * Represents an implicit receiver available in a particular [KaScopeContext].
 */
@KaExperimentalApi
public interface KaScopeImplicitReceiverValue : KaScopeImplicitValue {
    /**
     * The implicit value owner.
     */
    public val ownerSymbol: KaSymbol
}

/**
 * Represents an implicit argument available in a particular [KaScopeContext].
 */
@KaExperimentalApi
public interface KaScopeImplicitArgumentValue : KaScopeImplicitValue {
    /**
     * The corresponding context parameter symbol which can be used
     * as an implicit argument.
     */
    public val symbol: KaContextParameterSymbol
}

/**
 * Represents an implicit receiver available in a particular context.
 */
@OptIn(KaExperimentalApi::class)
public interface KaImplicitReceiver : KaScopeImplicitReceiverValue {
    override val type: KaType
    override val ownerSymbol: KaSymbol
    override val scopeIndexInTower: Int
}

public sealed interface KaScopeKind {
    /**
     * An index in the scope tower. The lower the index, the closer the scope is to the context position.
     *
     * #### Example
     *
     * ```kotlin
     * fun f(a: A, b: B) {      // local scope:       indexInTower = 2
     *     with(a) {            // type scope for A:  indexInTower = 1
     *         with(b) {        // type scope for B:  indexInTower = 0
     *             <caret>
     *         }
     *     }
     * }
     * ```
     */
    public val indexInTower: Int

    public interface LocalScope : KaScopeKind

    /**
     * Represents a [KaScope] for a type, which includes [synthetic Java properties](https://kotlinlang.org/docs/java-interop.html#getters-and-setters)
     * of that type.
     */
    public interface TypeScope : KaScopeKind

    public sealed interface NonLocalScope : KaScopeKind

    /**
     * Represents a [KaScope] containing type parameters.
     */
    public interface TypeParameterScope : NonLocalScope

    /**
     * Represents a [KaScope] containing declarations from a package.
     */
    public interface PackageMemberScope : NonLocalScope

    /**
     * Represents a [KaScope] containing declarations from imports.
     */
    public sealed interface ImportingScope : NonLocalScope

    /**
     * Represents a [KaScope] containing declarations from explicit non-star imports.
     */
    public interface ExplicitSimpleImportingScope : ImportingScope

    /**
     * Represents a [KaScope] containing declarations from explicit star imports.
     */
    public interface ExplicitStarImportingScope : ImportingScope

    /**
     * Represents a [KaScope] containing declarations from non-star imports which are not declared explicitly and are added by default.
     */
    public interface DefaultSimpleImportingScope : ImportingScope

    /**
     * Represents a [KaScope] containing declarations from star imports which are not declared explicitly and are added by default.
     */
    public interface DefaultStarImportingScope : ImportingScope

    /**
     * Represents a [KaScope] containing the static members of a classifier.
     */
    public interface StaticMemberScope : NonLocalScope

    /**
     * Represents a [KaScope] containing the members of a script.
     */
    public interface ScriptMemberScope : NonLocalScope
}

@KaIdeApi
public object KaScopeKinds {
    @KaIdeApi
    public class LocalScope(override val indexInTower: Int) : KaScopeKind.LocalScope

    @KaIdeApi
    public class TypeScope(override val indexInTower: Int) : KaScopeKind.TypeScope

    @KaIdeApi
    public class TypeParameterScope(override val indexInTower: Int) : KaScopeKind.TypeParameterScope

    @KaIdeApi
    public class PackageMemberScope(override val indexInTower: Int) : KaScopeKind.PackageMemberScope

    @KaIdeApi
    public class ExplicitSimpleImportingScope(override val indexInTower: Int) : KaScopeKind.ExplicitSimpleImportingScope

    @KaIdeApi
    public class ExplicitStarImportingScope(override val indexInTower: Int) : KaScopeKind.ExplicitStarImportingScope

    @KaIdeApi
    public class DefaultSimpleImportingScope(override val indexInTower: Int) : KaScopeKind.DefaultSimpleImportingScope

    @KaIdeApi
    public class DefaultStarImportingScope(override val indexInTower: Int) : KaScopeKind.DefaultStarImportingScope

    @KaIdeApi
    public class StaticMemberScope(override val indexInTower: Int) : KaScopeKind.StaticMemberScope

    @KaIdeApi
    public class ScriptMemberScope(override val indexInTower: Int) : KaScopeKind.ScriptMemberScope
}

/**
 * A wrapper around a [KaScope] which is additionally positioned in the scope tower of a [KaScopeContext], represented by [KaScopeKind].
 */
public interface KaScopeWithKind : KaLifetimeOwner {
    /**
     * The [KaScope] underlying this [KaScopeWithKind].
     */
    public val scope: KaScope

    /**
     * The kind of the scope derived from its position in the scope tower.
     */
    public val kind: KaScopeKind
}

@KaIdeApi
public class KaScopeWithKindImpl(
    private val backingScope: KaScope,
    private val backingKind: KaScopeKind,
) : KaScopeWithKind {
    override val token: KaLifetimeToken get() = backingScope.token

    override val scope: KaScope get() = withValidityAssertion { backingScope }
    override val kind: KaScopeKind get() = withValidityAssertion { backingKind }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaScopeWithKindImpl &&
                other.backingScope == backingScope &&
                other.backingKind == backingKind
    }

    override fun hashCode(): Int = Objects.hash(backingScope, backingKind)
}
