/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.StandardTypes.boolean
import org.jetbrains.kotlin.generators.tree.config.element
import org.jetbrains.kotlin.generators.tree.config.sealedElement
import org.jetbrains.kotlin.generators.tree.type
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.ir.generator.IrTree.anonymousInitializer
import org.jetbrains.kotlin.ir.generator.IrTree.call
import org.jetbrains.kotlin.ir.generator.IrTree.`class`
import org.jetbrains.kotlin.ir.generator.IrTree.classReference
import org.jetbrains.kotlin.ir.generator.IrTree.constructor
import org.jetbrains.kotlin.ir.generator.IrTree.constructorCall
import org.jetbrains.kotlin.ir.generator.IrTree.declaration
import org.jetbrains.kotlin.ir.generator.IrTree.enumEntry
import org.jetbrains.kotlin.ir.generator.IrTree.externalPackageFragment
import org.jetbrains.kotlin.ir.generator.IrTree.field
import org.jetbrains.kotlin.ir.generator.IrTree.fieldAccessExpression
import org.jetbrains.kotlin.ir.generator.IrTree.file
import org.jetbrains.kotlin.ir.generator.IrTree.function
import org.jetbrains.kotlin.ir.generator.IrTree.functionReference
import org.jetbrains.kotlin.ir.generator.IrTree.getEnumValue
import org.jetbrains.kotlin.ir.generator.IrTree.getField
import org.jetbrains.kotlin.ir.generator.IrTree.getValue
import org.jetbrains.kotlin.ir.generator.IrTree.localDelegatedProperty
import org.jetbrains.kotlin.ir.generator.IrTree.property
import org.jetbrains.kotlin.ir.generator.IrTree.`return`
import org.jetbrains.kotlin.ir.generator.IrTree.returnTarget
import org.jetbrains.kotlin.ir.generator.IrTree.returnableBlock
import org.jetbrains.kotlin.ir.generator.IrTree.script
import org.jetbrains.kotlin.ir.generator.IrTree.setField
import org.jetbrains.kotlin.ir.generator.IrTree.setValue
import org.jetbrains.kotlin.ir.generator.IrTree.simpleFunction
import org.jetbrains.kotlin.ir.generator.IrTree.symbolOwner
import org.jetbrains.kotlin.ir.generator.IrTree.typeAlias
import org.jetbrains.kotlin.ir.generator.IrTree.typeParameter
import org.jetbrains.kotlin.ir.generator.IrTree.valueDeclaration
import org.jetbrains.kotlin.ir.generator.IrTree.valueParameter
import org.jetbrains.kotlin.ir.generator.IrTree.variable
import org.jetbrains.kotlin.ir.generator.config.symbol.AbstractIrSymbolTreeBuilder
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.types.Variance
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

    private fun Symbol.bindableSymbolParent(descriptorTypeName: String, owner: Element) {
        val descriptorType = type(Packages.descriptors, descriptorTypeName)
        parent(bindableSymbol.withArgs("Descriptor" to descriptorType, "Owner" to owner))
        this.descriptor = descriptorType
        this.owner = owner
    }

    val packageFragmentSymbol by sealedElement {
        +descriptorField("PackageFragmentDescriptor")
    }

    val fileSymbol by element {
        parent(packageFragmentSymbol)
        bindableSymbolParent("PackageFragmentDescriptor", file)

        kDoc = """
        Such a symbol is always module-private.

        [${render()}] is never actually serialized, but is useful for deserializing private top-level declarations.

        See also: [${idSignatureType.render()}.FileSignature].
        """.trimIndent()
    }

    val externalPackageFragmentSymbol by element {
        parent(packageFragmentSymbol)
        bindableSymbolParent("PackageFragmentDescriptor", externalPackageFragment)
    }

    val anonymousInitializerSymbol by element {
        bindableSymbolParent("ClassDescriptor", anonymousInitializer)

        kDoc = """
        It's not very useful on its own, but since [${anonymousInitializer.render()}] is an [${declaration.render()}], and [${declaration.render()}]s must have symbols,
        here we are.
        
        This symbol is never public (wrt linkage).
        """.trimIndent()
    }

    val enumEntrySymbol by element {
        bindableSymbolParent("ClassDescriptor", enumEntry)
        parent(type<EnumEntrySymbolMarker>())
    }

    val fieldSymbol by element {
        bindableSymbolParent("PropertyDescriptor", field)
        parent(type<FieldSymbolMarker>())
    }

    val classifierSymbol by sealedElement {
        parent(type<TypeConstructorMarker>())

        +descriptorField("ClassifierDescriptor")
    }

    val classSymbol by element {
        parent(classifierSymbol)
        bindableSymbolParent("ClassDescriptor", `class`)
        parent(type<RegularClassSymbolMarker>())
    }

    val scriptSymbol by element {
        parent(classifierSymbol)
        bindableSymbolParent("ScriptDescriptor", script)
    }

    val typeParameterSymbol by element {
        parent(classifierSymbol)
        bindableSymbolParent("TypeParameterDescriptor", typeParameter)
        parent(type<TypeParameterMarker>())
        parent(type<TypeParameterSymbolMarker>())
    }

    val valueSymbol by sealedElement {
        +descriptorField("ValueDescriptor")
        +ownerField(valueDeclaration)
    }

    val valueParameterSymbol by element {
        parent(valueSymbol)
        bindableSymbolParent("ParameterDescriptor", valueParameter)
        parent(type<ValueParameterSymbolMarker>())
    }

    val variableSymbol by element {
        parent(valueSymbol)
        bindableSymbolParent("VariableDescriptor", variable)
    }

    val returnTargetSymbol by sealedElement {
        +descriptorField("FunctionDescriptor")
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
        bindableSymbolParent("ClassConstructorDescriptor", constructor)
        parent(type<ConstructorSymbolMarker>())
    }

    val simpleFunctionSymbol by element {
        parent(functionSymbol)
        bindableSymbolParent("FunctionDescriptor", simpleFunction)
        parent(type<SimpleFunctionSymbolMarker>())
    }

    val returnableBlockSymbol by element {
        parent(returnTargetSymbol)
        bindableSymbolParent("FunctionDescriptor", returnableBlock)
    }

    val propertySymbol by element {
        bindableSymbolParent("PropertyDescriptor", property)
        parent(type<PropertySymbolMarker>())
    }

    val localDelegatedPropertySymbol by element {
        bindableSymbolParent("VariableDescriptorWithAccessors", localDelegatedProperty)
    }

    val typeAliasSymbol by element {
        bindableSymbolParent("TypeAliasDescriptor", typeAlias)
        parent(type<TypeAliasSymbolMarker>())
    }
}