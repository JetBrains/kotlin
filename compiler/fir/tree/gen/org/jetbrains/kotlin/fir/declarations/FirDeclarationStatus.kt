/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * [FirDeclarationStatus] represents a storage of various modifers which are applicable to declarations.
 * It includes such information as visibility, modality, expect/actual flags and so on.
 *
 * This class has two inheritors, [FirCallableDeclarationStatus] and [FirClassLikeDeclarationStatus], which 
 * contain modifiers which are specific to callable declarations (e.g. `operator`) and class-like
 * declarations (e.g. `data`), respectively.
 *
 * [FirMemberDeclaration] has a property of type [FirDeclarationStatus], but its inheritors ([FirCallableDeclaration]
 * and [FirClassLikeDeclaration]) override it with more specific status type, so none of actual FIR declarations
 * exposes raw [FirDeclarationStatus].
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.declarationStatus]
 */
interface FirDeclarationStatus : FirElement {
    override val source: KtSourceElement?
    val visibility: Visibility
    val modality: Modality?
    val isExpect: Boolean
    val isActual: Boolean
    val isExternal: Boolean
    val isFromSealedClass: Boolean
    val isFromEnumClass: Boolean
    val defaultVisibility: Visibility
    val defaultModality: Modality

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitDeclarationStatus(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformDeclarationStatus(this, data) as E
}
