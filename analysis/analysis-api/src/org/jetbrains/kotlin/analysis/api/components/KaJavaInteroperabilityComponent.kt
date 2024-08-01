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
     * Converts the given [KaType] to [PsiType] under [useSitePosition] context.
     *
     * Note: [PsiType] is JVM conception, so this method will return `null` for non-JVM platforms.
     *
     * @receiver type to convert
     *
     * @param useSitePosition is used to determine if the given [KaType] needs to be approximated.
     * For instance, if the given type is local yet available in the same scope of use site,
     * we can still use such a local type.
     * Otherwise, e.g., exposed to public as a return type, the resulting type will be approximated accordingly.
     *
     * @param allowErrorTypes if **false** the result will be null in the case of an error type inside the [type][this].
     * Erroneous types will be replaced with `error.NonExistentClass` type.
     *
     * @param suppressWildcards indicates whether wild cards in type arguments need to be suppressed or not,
     * e.g., according to the annotation on the containing declarations.
     * - `true` means they should be suppressed.
     * - `false` means they should appear.
     * - `null` is no-op by default, i.e., their suppression/appearance is determined by type annotations.
     *
     * @param inferAnnotations if **true** the result [PsiType] will have converted annotations from the original [type][this]
     *
     * @param forceValueClassResolution if **false** and underlying [TypeMappingMode.needInlineClassWrapping] is **false** then
     * the result doesn't guarantee that a value class will be unwrapped.
     */
    @KaExperimentalApi
    public fun KaType.asPsiType(
        useSitePosition: PsiElement,
        allowErrorTypes: Boolean,
        mode: KaTypeMappingMode = KaTypeMappingMode.DEFAULT,
        isAnnotationMethod: Boolean = false,
        suppressWildcards: Boolean? = null,
        inferAnnotations: Boolean = false,
        forceValueClassResolution: Boolean = true,
    ): PsiType?

    /**
     * Converts given [PsiType] to [KaType].
     *
     * [useSitePosition] may be used to clarify how to resolve some parts of [PsiType].
     * For instance, it can be used to collect type parameters and use them during the conversion.
     *
     * @receiver [PsiType] to be converted.
     * @return The converted [KaType], or null if conversion is not possible e.g., [PsiType] is not resolved
     */
    @KaExperimentalApi
    public fun PsiType.asKaType(useSitePosition: PsiElement): KaType?

    @KaExperimentalApi
    @Deprecated("Use 'asKaType()' instead.", replaceWith = ReplaceWith("asKaType(useSitePosition)"))
    public fun PsiType.asKtType(useSitePosition: PsiElement): KaType? = asKaType(useSitePosition)

    /**
     * Create ASM JVM type by corresponding KaType
     *
     * @see TypeMappingMode
     */
    @KaExperimentalApi
    public fun KaType.mapToJvmType(mode: TypeMappingMode = TypeMappingMode.DEFAULT): Type

    @KaExperimentalApi
    @Deprecated("Use 'mapToJvmType()' instead.", replaceWith = ReplaceWith("mapToJvmType(mode)"))
    public fun KaType.mapTypeToJvmType(mode: TypeMappingMode = TypeMappingMode.DEFAULT): Type = mapToJvmType(mode)

    /**
     * `true` if the given type is backed by a single JVM primitive type.
     */
    @KaExperimentalApi
    public val KaType.isPrimitiveBacked: Boolean

    /**
     * Maps the given [PsiClass] declaration to a Kotlin class symbol.
     *
     * [namedClassSymbol] is always `null` for anonymous classes, type parameters (which are also [PsiClass]es),
     * and for Kotlin light classes.
     */
    public val PsiClass.namedClassSymbol: KaNamedClassSymbol?

    /**
     * Maps the given [PsiMember] method or field to a callable symbol.
     *
     * [callableSymbol] is always `null` for local declarations.
     */
    public val PsiMember.callableSymbol: KaCallableSymbol?

    /**
     * The containing JVM class name for the given [KaCallableSymbol].
     *
     * The property works for both source and library declarations.
     * The returned JVM class name is a fully qualified name separated by dots, e.g., `foo.bar.Baz.Companion`.
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