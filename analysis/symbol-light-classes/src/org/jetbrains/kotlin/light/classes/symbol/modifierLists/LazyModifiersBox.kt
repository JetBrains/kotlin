/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiModifier
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentHashMap
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.light.classes.symbol.analyzeForLightClasses
import org.jetbrains.kotlin.light.classes.symbol.computeSimpleModality
import org.jetbrains.kotlin.light.classes.symbol.restoreSymbolOrThrowIfDisposed
import org.jetbrains.kotlin.light.classes.symbol.toPsiVisibilityForMember
import org.jetbrains.kotlin.utils.keysToMap
import java.util.concurrent.atomic.AtomicReference

internal typealias LazyModifiersComputer = (modifier: String) -> Map<String, Boolean>?

internal class LazyModifiersBox(
    initialValue: Map<String, Boolean>,
    private val computer: LazyModifiersComputer,
) {
    private val modifiersMapReference: AtomicReference<PersistentMap<String, Boolean>> = AtomicReference(initialValue.toPersistentHashMap())

    fun hasModifier(modifier: String): Boolean {
        modifiersMapReference.get()[modifier]?.let { return it }
        val newValues = computer(modifier) ?: mapOf(modifier to false)
        modifiersMapReference.updateAndGet {
            it.putAll(newValues)
        }

        return newValues[modifier] ?: error("Inconsistent state: $modifier")
    }

    companion object {
        internal val VISIBILITY_MODIFIERS = setOf(PsiModifier.PUBLIC, PsiModifier.PACKAGE_LOCAL, PsiModifier.PROTECTED, PsiModifier.PRIVATE)
        internal val VISIBILITY_MODIFIERS_MAP: PersistentMap<String, Boolean> =
            VISIBILITY_MODIFIERS.keysToMap {
                false
            }.toPersistentHashMap()

        internal val MODALITY_MODIFIERS = setOf(PsiModifier.FINAL, PsiModifier.ABSTRACT)
        internal val MODALITY_MODIFIERS_MAP: PersistentMap<String, Boolean> =
            MODALITY_MODIFIERS.keysToMap {
                false
            }.toPersistentHashMap()

        internal fun computeVisibilityForMember(
            ktModule: KtModule,
            declarationPointer: KtSymbolPointer<KtSymbolWithVisibility>,
        ): PersistentMap<String, Boolean> {
            val visibility = analyzeForLightClasses(ktModule) {
                declarationPointer.restoreSymbolOrThrowIfDisposed().toPsiVisibilityForMember()
            }

            return VISIBILITY_MODIFIERS_MAP.with(visibility)
        }

        internal fun computeSimpleModality(
            ktModule: KtModule,
            declarationPointer: KtSymbolPointer<KtSymbolWithModality>,
        ): PersistentMap<String, Boolean> {
            val modality = analyzeForLightClasses(ktModule) {
                declarationPointer.restoreSymbolOrThrowIfDisposed().computeSimpleModality()
            }

            return MODALITY_MODIFIERS_MAP.with(modality)
        }
    }

}

@Suppress("NOTHING_TO_INLINE")
internal inline fun PersistentMap<String, Boolean>.with(modifier: String?): PersistentMap<String, Boolean> {
    return modifier?.let { put(modifier, true) } ?: this
}
