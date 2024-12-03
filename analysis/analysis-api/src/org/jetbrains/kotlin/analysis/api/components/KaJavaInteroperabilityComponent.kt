/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeMappingMode
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Type

public interface KaJavaInteroperabilityComponent {
    /**
     * Converts the given [KaType] to a [PsiType] in the context of the [useSitePosition].
     *
     * [PsiType] is JVM conception, so this method will return `null` for non-JVM platforms, unless [allowNonJvmPlatforms] is set.
     *
     * @receiver The [KaType] to convert.
     *
     * @param useSitePosition Determines whether the given [KaType] needs to be approximated.
     * For instance, if the given type is local but the use site is in the same local scope, we do not need to approximate the local type.
     * However, when exposed to the public as a return type, the resulting type must be approximated accordingly.
     *
     * @param allowErrorTypes Determines whether the [KaType] should still be converted if it contains an error type. When this option is
     * `false`, the result will be `null` if the [KaType] contains an error type. When `true`, erroneous types will be replaced with the
     * `error.NonExistentClass` type.
     *
     * @param suppressWildcards Indicates whether wildcards in type arguments should be suppressed. This option works similar to adding a
     * [JvmSuppressWildcards] annotation to the containing declaration.
     *
     * - `true` means they should be suppressed.
     * - `false` means they should appear.
     * - `null` means that the default applies, where wildcard suppression/appearance is determined by type annotations.
     *
     * @param preserveAnnotations Whether annotations from the original [KaType] should be included in the resulting [PsiType] with an
     * appropriate conversion.
     *
     * @param allowNonJvmPlatforms Whether the [PsiType] should be computed even for non-JVM modules. The flag provides no validity
     * guarantees – the returned type may be unresolvable from Java, or `null`.
     */
    @KaExperimentalApi
    public fun KaType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode = KaTypeMappingMode.DEFAULT,
        isAnnotationMethod: Boolean = false,
        suppressWildcards: Boolean? = null,
        preserveAnnotations: Boolean = true,
        allowNonJvmPlatforms: Boolean = false,
    ): PsiType?

    /**
     * Converts the given [PsiType] to a [KaType] in the context of the [useSitePosition].
     *
     * [useSitePosition] clarifies how to resolve some parts of the [PsiType]. For instance, it can be used to collect type parameters and
     * apply them during the conversion.
     *
     * @receiver The [PsiType] to be converted.
     *
     * @return The converted [KaType], or `null` if conversion is not possible. For example, [PsiType] might not be resolvable.
     */
    @KaExperimentalApi
    public fun PsiType.asKaType(useSitePosition: PsiElement): KaType?

    /**
     * Convert the given [KaType] to a JVM [ASM](https://asm.ow2.io) type.
     *
     * @see TypeMappingMode
     */
    @KaExperimentalApi
    public fun KaType.mapToJvmType(mode: TypeMappingMode = TypeMappingMode.DEFAULT): Type

    /**
     * Whether the given [KaType] is backed by a single JVM primitive type.
     */
    @KaExperimentalApi
    public val KaType.isPrimitiveBacked: Boolean

    /**
     * A [KaNamedClassSymbol] for the given [PsiClass], or `null` for anonymous classes, local classes, type parameters (which are also
     * [PsiClass]es), and Kotlin light classes.
     */
    public val PsiClass.namedClassSymbol: KaNamedClassSymbol?

    /**
     * A [KaCallableSymbol] for the given [PsiMember] method or field, or `null` for local declarations.
     */
    public val PsiMember.callableSymbol: KaCallableSymbol?

    /**
     * The containing JVM class name for the given [KaCallableSymbol].
     *
     * The property works for both source and library declarations.
     * The returned JVM class name is a fully qualified name separated by dots, such as `foo.bar.Baz.Companion`.
     *
     * Applicable only to JVM modules, and common modules with JVM targets.
     * [containingJvmClassName] is always `null` all other kinds of modules.
     */
    @KaExperimentalApi
    public val KaCallableSymbol.containingJvmClassName: String?

    /**
     * The JVM getter method name for the given [KaPropertySymbol].
     * The behavior is undefined for modules other than JVM and common (with a JVM implementation).
     */
    @KaExperimentalApi
    public val KaPropertySymbol.javaGetterName: Name

    /**
     * The JVM setter method name for the given [KaPropertySymbol].
     * The behavior is undefined for modules other than JVM and common (with a JVM implementation).
     */
    @KaExperimentalApi
    public val KaPropertySymbol.javaSetterName: Name?
}
