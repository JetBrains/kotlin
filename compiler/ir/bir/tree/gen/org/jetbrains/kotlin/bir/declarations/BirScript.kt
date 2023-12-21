/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/bir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.bir.declarations

import org.jetbrains.kotlin.bir.*
import org.jetbrains.kotlin.bir.expressions.BirStatementContainer
import org.jetbrains.kotlin.bir.symbols.BirClassSymbol
import org.jetbrains.kotlin.bir.symbols.BirPropertySymbol
import org.jetbrains.kotlin.bir.symbols.BirScriptSymbol
import org.jetbrains.kotlin.bir.types.BirType

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.bir.generator.BirTree.script]
 */
abstract class BirScript : BirImplElementBase(), BirElement, BirDeclaration, BirDeclarationWithName, BirDeclarationParent, BirStatementContainer, BirMetadataSourceOwner, BirScriptSymbol {
    abstract var thisReceiver: BirValueParameter?
    abstract var baseClass: BirType?
    abstract val explicitCallParameters: BirChildElementList<BirVariable>
    abstract val implicitReceiversParameters: BirChildElementList<BirValueParameter>
    abstract var providedProperties: List<BirPropertySymbol>
    abstract val providedPropertiesParameters: BirChildElementList<BirValueParameter>
    abstract var resultProperty: BirPropertySymbol?
    abstract var earlierScriptsParameter: BirValueParameter?
    abstract var importedScripts: List<BirScriptSymbol>?
    abstract var earlierScripts: List<BirScriptSymbol>?
    abstract var targetClass: BirClassSymbol?
    abstract var constructor: BirConstructor?

    override fun <D> acceptChildren(visitor: BirElementVisitor<D>, data: D) {
        annotations.acceptChildren(visitor, data)
        statements.acceptChildren(visitor, data)
        thisReceiver?.accept(data, visitor)
        explicitCallParameters.acceptChildren(visitor, data)
        implicitReceiversParameters.acceptChildren(visitor, data)
        providedPropertiesParameters.acceptChildren(visitor, data)
        earlierScriptsParameter?.accept(data, visitor)
    }

    companion object : BirElementClass(BirScript::class.java, 77, true)
}
