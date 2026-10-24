/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.PsiModifier
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.light.classes.symbol.utils.*
import org.jetbrains.kotlin.utils.keysToMap
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

internal typealias LazyModifiersComputer = (modifier: String) -> Map<String, Boolean>?

internal class GranularModifiersBox(
    initialValue: Map<String, Boolean> = emptyMap(),
    private val computer: LazyModifiersComputer,
) : ModifiersBox {
    /**
     * The computed modifiers which have a [bitIndex], as two bit sets in one value: the bit of a modifier in the high half tells that the
     * modifier is computed, and the same bit in the low half tells that it is present.
     *
     * There is a box for every modifier list of every light element, so the modifiers are not kept in a map, which takes much more memory.
     */
    @Volatile
    private var computedModifiers: Long = 0

    /**
     * The computed modifiers which have no [bitIndex].
     */
    @Volatile
    private var otherModifiers: PersistentMap<String, Boolean> = persistentHashMapOf()

    init {
        store(initialValue)
    }

    override fun hasModifier(modifier: String): Boolean {
        computedValue(modifier)?.let { return it }

        val newValues = computer(modifier) ?: mapOf(modifier to false)
        store(newValues)

        return computedValue(modifier) ?: error("Inconsistent state: $modifier")
    }

    private fun computedValue(modifier: String): Boolean? {
        val index = bitIndex(modifier)
        if (index < 0) return otherModifiers[modifier]

        val modifiers = computedModifiers
        val bit = 1L shl index
        return if (modifiers and (bit shl Int.SIZE_BITS) == 0L) null else modifiers and bit != 0L
    }

    /**
     * Stores [values] for the modifiers which are not computed yet. A value computed by another thread is kept, as both are the same.
     */
    private fun store(values: Map<String, Boolean>) {
        var computedBits = 0L
        var presentBits = 0L
        for ([modifier, value] in values) {
            val index = bitIndex(modifier)
            if (index < 0) continue

            val bit = 1L shl index
            computedBits = computedBits or (bit shl Int.SIZE_BITS)
            if (value) presentBits = presentBits or bit
        }

        while (computedBits != 0L) {
            val current = computedModifiers
            val newBits = computedBits and current.inv()
            val updated = current or newBits or (presentBits and (newBits ushr Int.SIZE_BITS))
            if (computedModifiersUpdater.compareAndSet(this, current, updated)) break
        }

        val others = values.filterKeys { bitIndex(it) < 0 }
        if (others.isNotEmpty()) {
            while (true) {
                val current = otherModifiers
                val updated = current.putAllAbsent(others)
                if (otherModifiersUpdater.compareAndSet(this, current, updated)) break
            }
        }
    }

    companion object {
        private val computedModifiersUpdater = AtomicLongFieldUpdater.newUpdater(
            /* tclass = */ GranularModifiersBox::class.java,
            /* fieldName = */ GranularModifiersBox::computedModifiers.name,
        )

        private val otherModifiersUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ GranularModifiersBox::class.java,
            /* vclass = */ PersistentMap::class.java,
            /* fieldName = */ GranularModifiersBox::otherModifiers.name,
        )

        /**
         * The index of the bit of [modifier] in [computedModifiers], or -1 if the modifier has none.
         */
        private fun bitIndex(modifier: String): Int = when (modifier) {
            PsiModifier.PUBLIC -> 0
            PsiModifier.PROTECTED -> 1
            PsiModifier.PRIVATE -> 2
            PsiModifier.PACKAGE_LOCAL -> 3
            PsiModifier.STATIC -> 4
            PsiModifier.ABSTRACT -> 5
            PsiModifier.FINAL -> 6
            PsiModifier.NATIVE -> 7
            PsiModifier.SYNCHRONIZED -> 8
            PsiModifier.STRICTFP -> 9
            PsiModifier.TRANSIENT -> 10
            PsiModifier.VOLATILE -> 11
            PsiModifier.DEFAULT -> 12
            PsiModifier.OPEN -> 13
            PsiModifier.TRANSITIVE -> 14
            PsiModifier.SEALED -> 15
            PsiModifier.NON_SEALED -> 16
            PsiModifier.VALUE -> 17
            else -> -1
        }

        private fun PersistentMap<String, Boolean>.putAllAbsent(values: Map<String, Boolean>): PersistentMap<String, Boolean> =
            values.entries.fold(this) { map, [modifier, value] -> if (modifier in map) map else map.putting(modifier, value) }

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
            useSiteModule: KaModule,
            declarationPointer: KaSymbolPointer<KaDeclarationSymbol>,
        ): PersistentMap<String, Boolean> {
            val visibility = declarationPointer.withSymbol(useSiteModule) {
                it.toPsiVisibilityForMember()
            }

            return VISIBILITY_MODIFIERS_MAP.with(visibility)
        }

        internal fun computeVisibilityForClass(
            useSiteModule: KaModule,
            declarationPointer: KaSymbolPointer<KaDeclarationSymbol>,
            isTopLevel: Boolean,
        ): PersistentMap<String, Boolean> {
            val visibility = declarationPointer.withSymbol(useSiteModule) {
                it.toPsiVisibilityForClass(!isTopLevel)
            }

            return VISIBILITY_MODIFIERS_MAP.with(visibility)
        }

        internal fun computeSimpleModality(
            useSiteModule: KaModule,
            declarationPointer: KaSymbolPointer<KaDeclarationSymbol>,
        ): PersistentMap<String, Boolean> {
            val modality = declarationPointer.withSymbol(useSiteModule) {
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
    return modifier?.let { putting(modifier, true) } ?: this
}
