/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtCallElement

/**
 * [KaAnnotation] is an application of an annotation to a declaration, type, or as an argument of another annotation.
 *
 * The annotation application may be unresolved, for example due to a type error. In such a case, properties like [classId] and
 * [constructorSymbol] might be `null`. However, the annotation application's [arguments] are available independently of its resolvability.
 *
 * #### Examples
 *
 * ```kotlin
 * // `@Deprecated` applied to a function declaration `foo1`.
 * @Deprecated("Should not be used")
 * fun foo1() {}
 *
 * // `@A` applied to a type `Int`.
 * fun foo2(x: List<@A Int>) {}
 *
 * // `B()` applied as an argument of another annotation `@A`.
 * @A(B())
 * fun foo3() {}
 * ```
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaAnnotation : KaLifetimeOwner {
    /**
     * The fully qualified annotation [ClassId], or `null` if the annotation application is unresolved.
     */
    public val classId: ClassId?

    /**
     * The [KtCallElement] which represents the application of the annotation to a declaration/type in PSI.
     *
     * [psi] is present only for declarations from sources. For declarations from other places (libraries, stdlib), it is `null`.
     */
    public val psi: KtCallElement?

    /**
     * The [annotation use-site target](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets) to which the annotation
     * was applied. It only exists for annotations applied to declarations.
     */
    public val useSiteTarget: AnnotationUseSiteTarget?

    /**
     * The list of annotation arguments passed to the [annotation constructor](https://kotlinlang.org/docs/annotations.html#constructors) in
     * the form of [KaNamedAnnotationValue]s.
     */
    public val arguments: List<KaNamedAnnotationValue>

    /**
     * The [KaConstructorSymbol] of the annotation's [constructor](https://kotlinlang.org/docs/annotations.html#constructors), or `null` if
     * the annotation application is unresolved.
     */
    public val constructorSymbol: KaConstructorSymbol?
}

/**
 * Represents an [annotation use-site target](https://kotlinlang.org/docs/annotations.html#annotation-use-site-targets) that specifies
 * which element a particular annotation applies to.
 *
 * In Kotlin, when a declaration (such as a property) maps to multiple underlying elements (backing field, getter, setter, etc.),
 * a use-site target disambiguates which element receives the annotation.
 *
 * #### Example:
 *
 * ```kotlin
 * class Person(@field:NotNull @get:JsonProperty("name") val name: String)
 * ```
 *
 * Here, `@field:NotNull` targets the backing field, while `@get:JsonProperty("name")` targets the getter.
 *
 * @property keyword The keyword used in source code for this use-site target (e.g., `"field"`, `"get"`, `"param"`).
 */
@KaExperimentalApi
public enum class KaAnnotationUseSiteTarget(public val keyword: String) {
    /**
     * A meta-target for properties that propagates the annotation to all applicable use sites, including the constructor parameter,
     * property, backing field, getter, and setter parameter.
     *
     * #### Example:
     *
     * ```kotlin
     * class Person(@all:NotNull val name: String)
     * ```
     */
    ALL("all"),

    /**
     * The backing field of a property.
     *
     * #### Example:
     *
     * ```kotlin
     * @field:NotNull
     * val name: String = ""
     * ```
     */
    FIELD("field"),

    /**
     * The file itself.
     * File-level annotations are placed at the top of the file, before the `package` statement.
     *
     * #### Example:
     *
     * ```kotlin
     * @file:JvmName("Utils")
     * package org.example
     * ```
     */
    FILE("file"),

    /**
     * The Kotlin property itself.
     * This target is not visible from Java.
     *
     * #### Example:
     *
     * ```kotlin
     * @property:Deprecated("Use 'fullName' instead")
     * val name: String = ""
     * ```
     */
    PROPERTY("property"),

    /**
     * The property getter.
     *
     * #### Example:
     *
     * ```kotlin
     * @get:JsonProperty("name")
     * val name: String = ""
     * ```
     */
    PROPERTY_GETTER("get"),

    /**
     * The property setter.
     *
     * #### Example:
     *
     * ```kotlin
     * @set:Inject
     * var dependency: Service? = null
     * ```
     */
    PROPERTY_SETTER("set"),

    /**
     * The extension receiver parameter of an extension function or property.
     *
     * #### Example:
     *
     * ```kotlin
     * fun @receiver:NotNull String.isNonEmpty(): Boolean = isNotEmpty()
     * ```
     */
    RECEIVER("receiver"),

    /**
     * A constructor parameter corresponding to a property declared in the primary constructor.
     *
     * #### Example:
     *
     * ```kotlin
     * class Person(@param:NotNull val name: String)
     * ```
     */
    CONSTRUCTOR_PARAMETER("param"),

    /**
     * The parameter of a property setter.
     *
     * #### Example:
     *
     * ```kotlin
     * @setparam:NotNull
     * var name: String = ""
     * ```
     */
    SETTER_PARAMETER("setparam"),

    /**
     * The field storing the delegate instance for a delegated property.
     *
     * #### Example:
     *
     * ```kotlin
     * @delegate:Inject
     * val lazyValue: String by lazy { "Hello" }
     * ```
     */
    PROPERTY_DELEGATE_FIELD("delegate");
}