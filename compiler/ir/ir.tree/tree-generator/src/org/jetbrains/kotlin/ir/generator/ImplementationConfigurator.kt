/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.Visibility
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.isSubclassOf
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.VariableKind
import org.jetbrains.kotlin.generators.tree.printer.printFunctionWithBlockBody
import org.jetbrains.kotlin.generators.tree.printer.printPropertyDeclaration
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.propertySymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.simpleFunctionSymbol
import org.jetbrains.kotlin.ir.generator.config.AbstractIrTreeImplementationConfigurator
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.utils.withIndent

object ImplementationConfigurator : AbstractIrTreeImplementationConfigurator() {
    override fun configure(model: Model): Unit = with(IrTree) {
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
        }.apply {
            // TODO: should be generated again after KT-68314 is fixed
            doPrint = false
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

        allImplOf(loop) {
            isLateinit("condition")
            defaultNull("label", "body")
        }

        allImplOf(breakContinue) {
            defaultNull("label")
        }

        impl(branch)

        impl(`when`) {
            default("branches", "ArrayList(2)")
        }

        impl(catch) {
            isLateinit("result")
        }

        impl(`try`) {
            isLateinit("tryResult")
            defaultNull("finallyExpression")
            default("catches", smartList())
        }

        impl(constantObject) {
            default("typeArguments", smartList())
            default("valueArguments", smartList())
        }

        impl(constantArray) {
            default("elements", smartList())
        }

        impl(dynamicOperatorExpression) {
            isLateinit("receiver")
            default("arguments", smartList())
        }

        impl(errorCallExpression) {
            defaultNull("explicitReceiver")
            default("arguments", smartList())
        }

        allImplOf(fieldAccessExpression) {
            defaultNull("receiver")
        }

        impl(setField) {
            isLateinit("value")
        }

        impl(stringConcatenation) {
            default("arguments", "ArrayList(2)")
        }

        impl(block)

        impl(returnableBlock) {
            default("descriptor", "symbol.descriptor", withGetter = true)
        }

        impl(errorExpression)

        impl(vararg) {
            default("elements", smartList())
        }


        impl(composite) {
            implementation.generationCallback = {
                println()
                print()
                println("""
                    // A temporary API for compatibility with Flysto user project, see KQA-1254
                    constructor(
                        startOffset: Int,
                        endOffset: Int,
                        type: IrType,
                        origin: IrStatementOrigin?,
                        statements: List<IrStatement>,
                    ) : this(
                        constructorIndicator = null,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = type,
                        origin = origin,
                    ) {
                        this.statements.addAll(statements)
                    }
                """.replaceIndent(currentIndent))
            }
        }

        impl(`return`) {
            implementation.generationCallback = {
                println()
                print()
                println("""
                    // A temporary API for compatibility with Flysto user project, see KQA-1254
                    constructor(
                        startOffset: Int,
                        endOffset: Int,
                        type: IrType,
                        returnTargetSymbol: IrReturnTargetSymbol,
                        value: IrExpression,
                    ) : this(
                        constructorIndicator = null,
                        startOffset = startOffset,
                        endOffset = endOffset,
                        type = type,
                        returnTargetSymbol = returnTargetSymbol,
                        value = value,
                    )
                """.replaceIndent(currentIndent))
            }
        }
    }

    private fun ImplementationContext.configureDeclarationWithLateBindinig(symbolType: Symbol) {
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
            fieldPredicate = { it is ListField && it.isChild && it.listType == StandardTypes.mutableList && it.implementationDefaultStrategy?.defaultValue == null }
        ) {
            default(it, "ArrayList()")
        }

        for (element in model.elements) {
            for (implementation in element.implementations) {
                // Generation of implementation classes of IrMemberAccessExpression are left out for subsequent MR, as a part of KT-65773.
                if (element == IrTree.const || element.isSubclassOf(IrTree.memberAccessExpression)) {
                    implementation.doPrint = false
                }

                if (element.category == Element.Category.Expression) {
                    implementation.isConstructorPublic = false
                }
            }
        }
    }
}
