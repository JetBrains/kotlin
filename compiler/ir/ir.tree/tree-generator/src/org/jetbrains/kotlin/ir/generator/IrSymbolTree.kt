/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.config.element
import org.jetbrains.kotlin.generators.tree.config.sealedElement
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.ir.generator.IrTree.anonymousInitializer
import org.jetbrains.kotlin.ir.generator.IrTree.`class`
import org.jetbrains.kotlin.ir.generator.IrTree.constructor
import org.jetbrains.kotlin.ir.generator.IrTree.declaration
import org.jetbrains.kotlin.ir.generator.IrTree.enumEntry
import org.jetbrains.kotlin.ir.generator.IrTree.externalPackageFragment
import org.jetbrains.kotlin.ir.generator.IrTree.field
import org.jetbrains.kotlin.ir.generator.IrTree.file
import org.jetbrains.kotlin.ir.generator.IrTree.function
import org.jetbrains.kotlin.ir.generator.IrTree.localDelegatedProperty
import org.jetbrains.kotlin.ir.generator.IrTree.property
import org.jetbrains.kotlin.ir.generator.IrTree.replSnippet
import org.jetbrains.kotlin.ir.generator.IrTree.returnTarget
import org.jetbrains.kotlin.ir.generator.IrTree.returnableBlock
import org.jetbrains.kotlin.ir.generator.IrTree.script
import org.jetbrains.kotlin.ir.generator.IrTree.simpleFunction
import org.jetbrains.kotlin.ir.generator.IrTree.typeAlias
import org.jetbrains.kotlin.ir.generator.IrTree.typeParameter
import org.jetbrains.kotlin.ir.generator.IrTree.valueDeclaration
import org.jetbrains.kotlin.ir.generator.IrTree.valueParameter
import org.jetbrains.kotlin.ir.generator.IrTree.variable
import org.jetbrains.kotlin.ir.generator.config.symbol.AbstractIrSymbolTreeBuilder
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker

@Suppress("unused", "MemberVisibilityCanBePrivate")
object IrSymbolTree : AbstractIrSymbolTreeBuilder() {

    override val rootElement: Symbol by element("Symbol") {
        // Written manually for simplicity
        doPrint = false
    }

    val bindableSymbol by element {
        // Written manually for simplicity
        doPrint = false
    }

    private fun Symbol.bindableSymbolParent(owner: Element) {
        parent(bindableSymbol.withArgs("Owner" to owner))
        this.owner = owner
    }

    val packageFragmentSymbol by sealedElement {
    }

    val fileSymbol by element {
        parent(packageFragmentSymbol)
        bindableSymbolParent(file)

        kDoc = """
        Such a symbol is always module-private.

        [${render()}] is never actually serialized, but is useful for deserializing private top-level declarations.

        See also: [${idSignatureType.render()}.FileSignature].
        """.trimIndent()
    }

    val externalPackageFragmentSymbol by element {
        parent(packageFragmentSymbol)
        bindableSymbolParent(externalPackageFragment)
    }

    val anonymousInitializerSymbol by element {
        bindableSymbolParent(anonymousInitializer)

        kDoc = """
        It's not very useful on its own, but since [${anonymousInitializer.render()}] is an [${declaration.render()}], and [${declaration.render()}]s must have symbols,
        here we are.
        
        This symbol is never public (wrt linkage).
        """.trimIndent()
    }

    val enumEntrySymbol by element {
        bindableSymbolParent(enumEntry)
        parent(type<EnumEntrySymbolMarker>())
    }

    val fieldSymbol by element {
        bindableSymbolParent(field)
        parent(type<FieldSymbolMarker>())
    }

    val classifierSymbol by sealedElement {
        parent(type<TypeConstructorMarker>())
    }

    val classSymbol by element {
        parent(classifierSymbol)
        bindableSymbolParent(`class`)
        parent(type<RegularClassSymbolMarker>())
    }

    val scriptSymbol by element {
        parent(classifierSymbol)
        bindableSymbolParent(script)
    }

    val replSnippetSymbol by element {
        parent(type<TypeConstructorMarker>())
        bindableSymbolParent(replSnippet)
    }

    val typeParameterSymbol by element {
        parent(classifierSymbol)
        bindableSymbolParent(typeParameter)
        parent(type<TypeParameterMarker>())
        parent(type<TypeParameterSymbolMarker>())
    }

    val valueSymbol by sealedElement {
        +ownerField(valueDeclaration)
    }

    val valueParameterSymbol by element {
        parent(valueSymbol)
        bindableSymbolParent(valueParameter)
        parent(type<ValueParameterSymbolMarker>())
    }

    val variableSymbol by element {
        parent(valueSymbol)
        bindableSymbolParent(variable)
    }

    val returnTargetSymbol by sealedElement {
        +ownerField(returnTarget)
    }

    val functionSymbol by sealedElement {
        parent(returnTargetSymbol)
        parent(type<FunctionSymbolMarker>())

        +ownerField(function)
        owner = function
    }

    val constructorSymbol by element {
        parent(functionSymbol)
        bindableSymbolParent(constructor)
        parent(type<ConstructorSymbolMarker>())
    }

    val simpleFunctionSymbol by element {
        parent(functionSymbol)
        bindableSymbolParent(simpleFunction)
        parent(type<SimpleFunctionSymbolMarker>())
    }

    val returnableBlockSymbol by element {
        parent(returnTargetSymbol)
        bindableSymbolParent(returnableBlock)
    }

    val declarationWithAccessorsSymbol by element {
        isSealed = true
    }

    val propertySymbol by element {
        bindableSymbolParent(property)
        parent(declarationWithAccessorsSymbol)
        parent(type<PropertySymbolMarker>())
    }

    val localDelegatedPropertySymbol by element {
        parent(declarationWithAccessorsSymbol)
        bindableSymbolParent(localDelegatedProperty)
    }

    val typeAliasSymbol by element {
        bindableSymbolParent(typeAlias)
        parent(type<TypeAliasSymbolMarker>())
    }
}
