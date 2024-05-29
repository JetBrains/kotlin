/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator

import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.propertySymbol
import org.jetbrains.kotlin.bir.generator.BirSymbolTree.simpleFunctionSymbol
import org.jetbrains.kotlin.bir.generator.config.AbstractIrTreeImplementationConfigurator
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.withIndent

object ImplementationConfigurator : AbstractIrTreeImplementationConfigurator() {
    override fun configure(model: Model): Unit = with(BirTree) {
        allImplOf(attributeContainer) {
            default("attributeOwnerId", "this")
        }

        allImplOf(mutableAnnotationContainer) {
            defaultEmptyList("annotations")
        }

        allImplOf(symbolOwner) {
            defaultNull("signature")
        }

        allImplOf(overridableDeclaration) {
            defaultEmptyList("overriddenSymbols")
        }

        allImplOf(typeParametersContainer) {
            defaultEmptyList("typeParameters")
        }

        allImplOf(function) {
            defaultEmptyList("valueParameters")
            defaultNull("dispatchReceiverParameter", "extensionReceiverParameter", "body")
            default("contextReceiverParametersCount", "0")
            isLateinit("returnType")
        }

        allImplOf(simpleFunction) {
            defaultNull("correspondingPropertySymbol")
        }

        impl(simpleFunction)

        impl(functionWithLateBinding) {
            configureDeclarationWithLateBindinig(simpleFunctionSymbol)
        }

        impl(field) {
            defaultNull("initializer", "correspondingPropertySymbol")
        }

        allImplOf(property) {
            defaultNull("backingField", "getter", "setter")
        }

        impl(property)

        impl(propertyWithLateBinding) {
            configureDeclarationWithLateBindinig(propertySymbol)
        }

        impl(typeParameter) {
            defaultEmptyList("superTypes")
        }

        impl(valueParameter) {
            defaultNull("defaultValue")
        }

        impl(variable) {
            implementation.putImplementationOptInInConstructor = false
            implementation.constructorParameterOrderOverride =
                listOf("origin", "name", "type", "isVar", "isConst", "isLateinit")
            defaultNull("initializer")
        }

        impl(`class`) {
            kind = ImplementationKind.OpenClass
            defaultNull("thisReceiver", "valueClassRepresentation")
            defaultEmptyList("superTypes")
            defaultFalse("isExternal", "isCompanion", "isInner", "isData", "isValue", "isExpect", "isFun", "hasEnumEntries")
        }

        impl(enumEntry) {
            defaultNull("correspondingClass", "initializerExpression")
        }

        impl(script) {
            implementation.putImplementationOptInInConstructor = false
            defaultNull(
                "thisReceiver", "baseClass", "resultProperty", "earlierScriptsParameter",
                "importedScripts", "earlierScripts", "targetClass", "constructor"
            )
            //default("origin", "SCRIPT_ORIGIN")
        }

        impl(moduleFragment) {
            implementation.putImplementationOptInInConstructor = false
            default("name", "descriptor.name", withGetter = true)
        }

        impl(errorDeclaration) {
            implementation.bindOwnedSymbol = false
            default("symbol") {
                value = "error(\"Should never be called\")"
                withGetter = true
            }
        }

        impl(externalPackageFragment) {
            implementation.putImplementationOptInInConstructor = false
            implementation.constructorParameterOrderOverride = listOf("packageFqName")
        }

        impl(file) {
            implementation.putImplementationOptInInConstructor = false
            implementation.constructorParameterOrderOverride = listOf("fileEntry", "packageFqName")
        }

        allImplOf(loop) {
            defaultNull("label", "body")
        }

        allImplOf(breakContinue) {
            defaultNull("label")
        }

        impl(branch)

        impl(`when`)

        impl(`try`) {
            defaultNull("finallyExpression")
        }

        impl(constantObject) {
            default("typeArguments", smartList())
        }

        impl(errorCallExpression) {
            defaultNull("explicitReceiver")
        }

        allImplOf(fieldAccessExpression) {
            defaultNull("receiver")
        }

        impl(block)

        impl(errorExpression)
    }

    private fun ImplementationContext.configureDeclarationWithLateBindinig(symbolType: Symbol) {
        val boundableSymbolType = ClassRef<TypeParameterRef>(TypeKind.Class, Packages.symbols, "LateBoundBirSymbol", symbolType.name.removePrefix("Bir"))

        implementation.bindOwnedSymbol = false
        default("isBound") {
            value = "_symbol != null"
            withGetter = true
        }
        additionalImports(ArbitraryImportable("org.jetbrains.kotlin.ir.descriptors", "toIrBasedDescriptor"))
        implementation.generationCallback = {
            println()
            printPropertyDeclaration(
                "_symbol",
                symbolType.copy(nullable = true),
                VariableKind.VAR,
                visibility = Visibility.PRIVATE,
                initializer = "null"
            )
            println()
            println()
            printFunctionWithBlockBody(
                "acquireSymbol",
                listOf(FunctionParameter("symbol", boundableSymbolType)),
                implementation.element,
            ) {
                println("assert(_symbol == null) { \"\$this already has symbol _symbol\" }")
                println("_symbol = symbol")
                println("symbol.bind(this)")
                println("return this")
            }
        }
    }

    override fun configureAllImplementations(model: Model) {
        configureFieldInAllImplementations(
            fieldName = null,
            fieldPredicate = { it is ListField && it.isChild }
        ) {
            default(it, "CHILD LIST IMPL INTRINSIC")
        }
    }
}
