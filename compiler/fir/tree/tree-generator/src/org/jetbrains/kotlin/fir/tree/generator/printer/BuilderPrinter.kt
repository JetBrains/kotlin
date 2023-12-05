/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.declarationAttributesType
import org.jetbrains.kotlin.fir.tree.generator.firBuilderDslAnnotation
import org.jetbrains.kotlin.fir.tree.generator.firImplementationDetailType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.FieldWithDefault
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.fir.tree.generator.toMutableOrEmptyImport
import org.jetbrains.kotlin.generators.tree.AbstractBuilderPrinter
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.utils.SmartPrinter

internal class BuilderPrinter(printer: SmartPrinter) : AbstractBuilderPrinter<Element, Implementation, FieldWithDefault, Field>(printer) {

    override val implementationDetailAnnotation: ClassRef<*>
        get() = firImplementationDetailType

    override val builderDslAnnotation: ClassRef<*>
        get() = firBuilderDslAnnotation

    override fun actualTypeOfField(field: Field) = field.getMutableType(forBuilder = true)

    context(ImportCollector)
    override fun SmartPrinter.printFieldReferenceInImplementationConstructorCall(field: FieldWithDefault) {
        print(field.name)
        if (field.isMutableOrEmptyList) {
            addImport(toMutableOrEmptyImport)
            print(".toMutableOrEmpty()")
        }
    }

    context(ImportCollector)
    override fun copyField(
        field: FieldWithDefault,
        originalParameterName: String,
        copyBuilderVariableName: String
    ) {
        if (field.typeRef == declarationAttributesType) {
            printer.println(copyBuilderVariableName, ".", field.name, " = ", originalParameterName, ".", field.name, ".copy()")
        } else {
            super.copyField(field, originalParameterName, copyBuilderVariableName)
        }
    }
}
