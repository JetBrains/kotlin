/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiModifier
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentHashMap
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.utils.keysToMap
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

internal typealias LazyModifiersComputer = (modifier: String) -> Map<String, Boolean>?

internal class GranularModifiersBox(
    initialValue: Map<String, Boolean> = emptyMap(),
    private val computer: LazyModifiersComputer,
) : ModifiersBox {
    @Volatile
    private var modifiersMapReference: PersistentMap<String, Boolean> = initialValue.toPersistentHashMap()

    override fun hasModifier(modifier: String): Boolean {
        modifiersMapReference[modifier]?.let { return it }

        val newValues = computer(modifier) ?: mapOf(modifier to false)
        do {
            val currentMap = modifiersMapReference
            currentMap[modifier]?.let { return it }

            val newMap = currentMap.putAll(newValues)
        } while (fieldUpdater.weakCompareAndSet(/* obj = */ this, /* expect = */ currentMap, /* update = */ newMap))

        return newValues[modifier] ?: error("Inconsistent state: $modifier")
    }

    companion object {
        private val fieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularModifiersBox::class.java,
            /* vclass = */ PersistentMap::class.java,
            /* fieldName = */ "modifiersMapReference",
        )

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
            ktModule: KaModule,
            declarationPointer: KaSymbolPointer<KaDeclarationSymbol>,
        ): PersistentMap<String, Boolean> {
            val visibility = declarationPointer.withSymbol(ktModule) {
                it.toPsiVisibilityForMember()
            }

            return VISIBILITY_MODIFIERS_MAP.with(visibility)
        }

        internal fun computeVisibilityForClass(
            ktModule: KaModule,
            declarationPointer: KaSymbolPointer<KaDeclarationSymbol>,
            isTopLevel: Boolean,
        ): PersistentMap<String, Boolean> {
            val visibility = declarationPointer.withSymbol(ktModule) {
                it.toPsiVisibilityForClass(!isTopLevel)
            }

            return VISIBILITY_MODIFIERS_MAP.with(visibility)
        }

        internal fun computeSimpleModality(
            ktModule: KaModule,
            declarationPointer: KaSymbolPointer<KaDeclarationSymbol>,
        ): PersistentMap<String, Boolean> {
            val modality = declarationPointer.withSymbol(ktModule) {
                if ((it as? KaClassSymbol)?.classKind == KaClassKind.ENUM_CLASS) {
                    enumClassModality(it)
                } else {
                    it.computeSimpleModality()
                }
            }

            return MODALITY_MODIFIERS_MAP.with(modality)
        }
    }

}

@Suppress("NOTHING_TO_INLINE")
internal inline fun PersistentMap<String, Boolean>.with(modifier: String?): PersistentMap<String, Boolean> {
    return modifier?.let { put(modifier, true) } ?: this
}
