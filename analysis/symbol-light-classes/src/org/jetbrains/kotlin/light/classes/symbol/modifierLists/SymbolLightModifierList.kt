/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.modifierLists

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentHashMap
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithModality
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.*
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.utils.keysToMap
import java.util.concurrent.atomic.AtomicReference

internal sealed class SymbolLightModifierList<out T : KtLightElement<KtModifierListOwner, PsiModifierListOwner>> :
    KtLightElementBase, PsiModifierList, KtLightElement<KtModifierList, PsiModifierListOwner> {
    protected val owner: T
    private val staticModifiers: Set<String>?
    private val lazyModifiersBox: LazyModifiersBox?
    private val lazyAnnotations: Lazy<List<PsiAnnotation>>?

    constructor(
        owner: T,
        initialValue: Map<String, Boolean>,
        lazyModifiersComputer: LazyModifiersComputer,
        annotationsComputer: ((PsiModifierList) -> List<PsiAnnotation>)?,
    ) : super(owner) {
        this.owner = owner
        this.lazyAnnotations = annotationsComputer?.let { lazyPub { annotationsComputer(this) } }

        this.lazyModifiersBox = LazyModifiersBox(initialValue, lazyModifiersComputer)
        this.staticModifiers = null
    }

    constructor(
        owner: T,
        staticModifiers: Set<String>,
        annotationsComputer: ((PsiModifierList) -> List<PsiAnnotation>)?,
    ) : super(owner) {
        this.owner = owner
        this.lazyAnnotations = annotationsComputer?.let { lazyPub { annotationsComputer(this) } }

        this.lazyModifiersBox = null
        this.staticModifiers = staticModifiers
    }

    override val kotlinOrigin: KtModifierList? get() = owner.kotlinOrigin?.modifierList
    override fun getParent() = owner
    override fun setModifierProperty(name: String, value: Boolean) = cannotModify()
    override fun checkSetModifierProperty(name: String, value: Boolean) = throw IncorrectOperationException()
    override fun addAnnotation(qualifiedName: String): PsiAnnotation = cannotModify()
    override fun getApplicableAnnotations(): Array<out PsiAnnotation> = annotations
    override fun isEquivalentTo(another: PsiElement?) = another is SymbolLightModifierList<*> && owner == another.owner
    override fun isWritable() = false
    override fun toString() = "Light modifier list of $owner"

    override val givenAnnotations: List<KtLightAbstractAnnotation> get() = invalidAccess()

    override fun getAnnotations(): Array<out PsiAnnotation> = lazyAnnotations?.value?.toTypedArray() ?: PsiAnnotation.EMPTY_ARRAY
    override fun findAnnotation(qualifiedName: String): PsiAnnotation? =
        lazyAnnotations?.value?.firstOrNull { it.qualifiedName == qualifiedName }

    override fun equals(other: Any?): Boolean = this === other || other is SymbolLightModifierList<*> && other.kotlinOrigin == kotlinOrigin

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun hasExplicitModifier(name: String) = hasModifierProperty(name)
    override fun hasModifierProperty(name: String): Boolean =
        staticModifiers?.contains(name) == true || lazyModifiersBox?.hasModifier(name) == true
}

internal typealias LazyModifiersComputer = (modifier: String) -> Map<String, Boolean>?

@Suppress("NOTHING_TO_INLINE")
internal inline fun PersistentMap<String, Boolean>.with(modifier: String?): PersistentMap<String, Boolean> {
    return modifier?.let { put(modifier, true) } ?: this
}

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
