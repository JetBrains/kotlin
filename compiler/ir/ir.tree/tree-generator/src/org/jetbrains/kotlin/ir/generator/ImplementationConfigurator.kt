/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.ir.generator.config.AbstractIrTreeImplementationConfigurator
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.utils.withIndent

object ImplementationConfigurator : AbstractIrTreeImplementationConfigurator() {
    override fun configure(model: Model): Unit = with(IrTree) {
        allImplOf(declaration) {
            isLateinit("parent")
            isMutable("parent")
        }

        allImplOf(attributeContainer) {
            default("attributeOwnerId", "this")
            defaultNull("originalBeforeInline")
        }

        allImplOf(metadataSourceOwner) {
            defaultNull("metadata")
        }

        allImplOf(mutableAnnotationContainer) {
            defaultEmptyList("annotations")
        }

        allImplOf(overridableDeclaration) {
            defaultEmptyList("overriddenSymbols")
        }

        allImplOf(typeParametersContainer) {
            defaultEmptyList("typeParameters")
        }

        allImplOf(statementContainer) {
            default("statements", "ArrayList(2)")
        }

        allImplOf(declaration) {
            default("descriptor", "symbol.descriptor", withGetter = true)
        }

        impl(anonymousInitializer) {
            isLateinit("body")
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

        impl(simpleFunction, "IrFunctionImpl")

        impl(functionWithLateBinding) {
            configureDeclarationWithLateBindinig(simpleFunctionSymbolType)
        }

        impl(field) {
            defaultNull("initializer", "correspondingPropertySymbol")
        }

        allImplOf(property) {
            defaultNull("backingField", "getter", "setter")
        }

        impl(property)

        impl(propertyWithLateBinding) {
            configureDeclarationWithLateBindinig(propertySymbolType)
        }

        impl(localDelegatedProperty) {
            isLateinit("delegate", "getter")
            defaultNull("setter")
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
                listOf("startOffset", "endOffset", "origin", "symbol", "name", "type", "isVar", "isConst", "isLateinit")
            defaultNull("initializer")
            default("factory") {
                value = "error(\"Create IrVariableImpl directly\")"
                withGetter = true
            }
        }

        impl(`class`) {
            kind = ImplementationKind.OpenClass
            defaultNull("thisReceiver", "valueClassRepresentation")
            defaultEmptyList("superTypes", "sealedSubclasses")
            defaultFalse("isExternal", "isCompanion", "isInner", "isData", "isValue", "isExpect", "isFun", "hasEnumEntries")
        }

        impl(enumEntry) {
            defaultNull("correspondingClass", "initializerExpression")
        }

        impl(script) {
            implementation.putImplementationOptInInConstructor = false
            implementation.constructorParameterOrderOverride = listOf("symbol", "name", "factory", "startOffset", "endOffset")
            defaultNull(
                "thisReceiver", "baseClass", "resultProperty", "earlierScriptsParameter",
                "importedScripts", "earlierScripts", "targetClass", "constructor"
            )
            isLateinit("explicitCallParameters", "implicitReceiversParameters", "providedProperties", "providedPropertiesParameters")
            default("origin", "SCRIPT_ORIGIN")
        }

        impl(moduleFragment) {
            implementation.putImplementationOptInInConstructor = false
            default("startOffset", undefinedOffset(), withGetter = true)
            default("endOffset", undefinedOffset(), withGetter = true)
            default("name", "descriptor.name", withGetter = true)
        }

        impl(errorDeclaration) {
            implementation.bindOwnedSymbol = false
            default("symbol") {
                value = "error(\"Should never be called\")"
                withGetter = true
            }
            isMutable("descriptor")
            isLateinit("descriptor")
        }

        impl(externalPackageFragment) {
            implementation.putImplementationOptInInConstructor = false
            implementation.constructorParameterOrderOverride = listOf("symbol", "packageFqName")
            additionalImports(
                ArbitraryImportable(Packages.descriptors, "ModuleDescriptor"),
            )
            default("startOffset", undefinedOffset(), withGetter = true)
            default("endOffset", undefinedOffset(), withGetter = true)
            implementation.generationCallback = {
                println()
                printlnMultiLine(
                    """
                    companion object {
                        @Deprecated(
                            message = "Use org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment instead",
                            replaceWith = ReplaceWith("createEmptyExternalPackageFragment", "org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment")
                        )
                        fun createEmptyExternalPackageFragment(module: ModuleDescriptor, fqName: FqName): IrExternalPackageFragment =
                            org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment(module, fqName)
                    }
                    """
                )
            }
        }

        impl(file) {
            implementation.putImplementationOptInInConstructor = false
            implementation.constructorParameterOrderOverride = listOf("fileEntry", "symbol", "packageFqName")
            default("startOffset", "0", withGetter = true)
            default("endOffset", "fileEntry.maxOffset", withGetter = true)
            isMutable("module")
            isLateinit("module")
            implementation.generationCallback = {
                println()
                println("internal val isInsideModule: Boolean")
                withIndent {
                    println("get() = ::module.isInitialized")
                }
            }
        }
    }

    private fun ImplementationContext.configureDeclarationWithLateBindinig(symbolType: ClassRef<*>) {
        implementation.bindOwnedSymbol = false
        default("isBound") {
            value = "_symbol != null"
            withGetter = true
        }
        default("symbol") {
            value = "_symbol ?: error(\"\$this has not acquired a symbol yet\")"
            withGetter = true
        }
        additionalImports(ArbitraryImportable("org.jetbrains.kotlin.ir.descriptors", "toIrBasedDescriptor"))
        default("descriptor") {
            value = "_symbol?.descriptor ?: this.toIrBasedDescriptor()"
            withGetter = true
        }
        defaultNull("containerSource", withGetter = true)
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
                listOf(FunctionParameter("symbol", symbolType)),
                implementation.element,
                override = true,
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
            fieldPredicate = { it is ListField && it.isChild && it.listType == StandardTypes.mutableList }
        ) {
            default(it, "ArrayList()")
        }

        // Generation of implementation classes of IrExpression are left out for subsequent MR, as a part of KT-65773.
        for (element in model.elements) {
            if (element.category == Element.Category.Expression) {
                for (implementation in element.implementations) {
                    implementation.doPrint = false
                }
            }
        }
    }
}