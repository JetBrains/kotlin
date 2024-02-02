/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.bir.util.BirImplementationDetail

abstract class BirScript() : BirImplElementBase(), BirDeclarationBase, BirDeclarationWithName, BirDeclarationParent, BirStatementContainer, BirMetadataSourceOwner {
    abstract override val symbol: BirScriptSymbol

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
        statements.acceptChildren(visitor, data)
        thisReceiver?.accept(data, visitor)
        explicitCallParameters.acceptChildren(visitor, data)
        implicitReceiversParameters.acceptChildren(visitor, data)
        providedPropertiesParameters.acceptChildren(visitor, data)
        earlierScriptsParameter?.accept(data, visitor)
    }

    @BirImplementationDetail
    override fun getElementClassInternal(): BirElementClass<*> = BirScript

    companion object : BirElementClass<BirScript>(BirScript::class.java, 80, true)
}
