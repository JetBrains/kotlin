/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.model

import org.jetbrains.kotlin.bir.generator.Model
import org.jetbrains.kotlin.bir.generator.BirTree
import org.jetbrains.kotlin.generators.tree.*

fun Model.adjustSymbolOwners() {
    for (element in elements) {
        if (element.isSubclassOf(BirTree.symbolOwner)
        //&& element != BirTree.functionWithLateBinding && element != BirTree.propertyWithLateBinding
        ) {
            val symbolField = element.fields.firstOrNull { it.symbolFieldRole == AbstractField.SymbolFieldRole.DECLARED }
            if (symbolField != null) {
                //element.fields.remove(symbolField)

                val symbolType = when (val type = symbolField.typeRef) {
                    is ElementOrRef<*> -> type
                    is NamedTypeParameterRef -> (type.origin as TypeVariable).bounds.single() as ElementOrRef<*>
                    else -> error(type)
                }
                //val symbolType = symbolField.symbolClass!!

                element.elementDescendantsAndSelfDepthFirst().forEach { descendantElement ->
                    if (descendantElement != BirTree.functionWithLateBinding && descendantElement != BirTree.propertyWithLateBinding) {
                        descendantElement.ownerSymbolType = symbolType

                        descendantElement.implementations.forEach { implementation ->
                            implementation[symbolField.name].implementationDefaultStrategy =
                                AbstractField.ImplementationDefaultStrategy.DefaultValue("this", true)
                        }
                    }
                }

                //element.otherParents += ClassRef<TypeParameterRef>(symbolType.typeKind, symbolType.packageName, symbolType.typeName)
            }
        }
    }
}

fun Model.setClassIds() {
    var id = 0
    elements.sortedBy { it.name }
        .sortedByDescending { it.implementations.isNotEmpty() } // Optimization for dispatching by class
        .sortedByDescending { it == rootElement }
        .forEach { element ->
            element.classId = id++
        }
}


fun Model.computeFieldProperties() {
    for (element in elements) {
        for (field in element.allFields) {
            field.isReadWriteTrackedProperty = field.isMutable && !(field is ListField && field.isChild)
        }
    }
}
