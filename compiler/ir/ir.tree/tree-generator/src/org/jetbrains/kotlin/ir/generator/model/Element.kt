/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.elementBaseType
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef
import org.jetbrains.kotlin.generators.tree.ElementRef as GenericElementRef

class Element(
    name: String,
    override val propertyName: String,
    val category: Category,
) : AbstractElement<Element, Field, Implementation>(name) {

    enum class Category(private val packageDir: String, val defaultVisitorParam: String) {
        Expression("expressions", "expression"),
        Declaration("declarations", "declaration"),
        Other("", "element");

        val packageName: String get() = BASE_PACKAGE + if (packageDir.isNotEmpty()) ".$packageDir" else ""
    }

    override val packageName: String = category.packageName

    /**
     * Allows to forcibly skip generation of the method for this element in visitors.
     */
    var generateVisitorMethod = true

    override val parentInVisitor: Element?
        get() {
            if (!generateVisitorMethod) return null
            return customParentInVisitor
                ?: elementParents.singleOrNull { it.typeKind == TypeKind.Class }?.element
                ?: IrTree.rootElement.takeIf { elementBaseType in otherParents }
        }


    var typeKind: TypeKind? = null
        set(value) {
            kind = when (value) {
                TypeKind.Class -> ImplementationKind.AbstractClass
                TypeKind.Interface -> ImplementationKind.Interface
                null -> null
            }
            field = value
        }

    override val namePrefix: String
        get() = "Ir"

    override var childrenOrderOverride: List<String>? = null

    override var visitorParameterName = category.defaultVisitorParam

    var customHasAcceptMethod: Boolean? = null

    override val hasAcceptMethod: Boolean
        get() = customHasAcceptMethod ?: (implementations.isNotEmpty() && parentInVisitor != null)


    override var hasTransformMethod = false

    override val hasAcceptChildrenMethod: Boolean
        get() = hasAcceptOrTransformChildrenMethod(Element::walkableChildren)

    override val hasTransformChildrenMethod: Boolean
        get() = hasAcceptOrTransformChildrenMethod(Element::transformableChildren)

    private fun hasAcceptOrTransformChildrenMethod(walkableOrTransformableChildren: Element.() -> List<Field>): Boolean {
        if (!ownsChildren) return false
        if (!isRootElement && walkableOrTransformableChildren().isEmpty()) return false
        val atLeastOneParentHasAcceptOrTransformChildrenMethod = elementAncestorsAndSelfDepthFirst().any { parent ->
            parent != this && parent.hasAcceptOrTransformChildrenMethod(walkableOrTransformableChildren) && !parent.isRootElement
        }
        return !atLeastOneParentHasAcceptOrTransformChildrenMethod
    }

    var transformByChildren = false
    var ownsChildren = true // If false, acceptChildren/transformChildren will NOT be generated.

    var generationCallback: (ImportCollectingPrinter.() -> Unit)? = null
}
