/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

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
