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
                print()
                println(
                    """
                    companion object {
                        @Deprecated(
                            message = "Use org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment instead",
                            replaceWith = ReplaceWith("createEmptyExternalPackageFragment", "org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment")
                        )
                        fun createEmptyExternalPackageFragment(module: ModuleDescriptor, fqName: FqName): IrExternalPackageFragment =
                            org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment(module, fqName)
                    }
                    """.replaceIndent(currentIndent)
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

        impl(catch) {
            isLateinit("result")
        }

        impl(`try`) {
            isLateinit("tryResult")
            defaultNull("finallyExpression")
        }

        impl(constantObject) {
            default("typeArguments", smartList())
        }

        impl(dynamicOperatorExpression) {
            isLateinit("receiver")
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

        impl(returnableBlock) {
            default("descriptor", "symbol.descriptor", withGetter = true)
        }

        impl(block)

        impl(branch)

        impl(errorExpression)

        impl(composite) {
            additionalImports(ArbitraryImportable("org.jetbrains.kotlin.ir", "IrStatement"))
            implementation.generationCallback = {
                println()
                print()
                println(
                    """
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
                """.replaceIndent(currentIndent)
                )
            }
        }

        impl(`return`) {
            implementation.generationCallback = {
                println()
                print()
                println(
                    """
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
                """.replaceIndent(currentIndent)
                )
            }
        }


        allImplOf(memberAccessExpression) {
            defaultNull("dispatchReceiver", "extensionReceiver")
        }

        allImplOf(functionAccessExpression) {
            default("contextReceiversCount", "0")
        }

        impl(call) {
            implementation.generationCallback = {
                println()
                print()
                println(
                    """
                companion object {
                    // Temporary API for compatible-compose, to be removed soon.
                    // Note: It cannot be marked with @Deprecated, because some usages in kotlin compiler pick this declaration up while it still exists.
                    fun fromSymbolOwner(
                        startOffset: Int,
                        endOffset: Int,
                        type: IrType,
                        symbol: IrSimpleFunctionSymbol,
                        typeArgumentsCount: Int = symbol.owner.typeParameters.size,
                        valueArgumentsCount: Int = symbol.owner.valueParameters.size,
                        origin: IrStatementOrigin? = null,
                        superQualifierSymbol: IrClassSymbol? = null,
                    ) =
                        IrCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount, valueArgumentsCount, origin, superQualifierSymbol)
                }
                """.replaceIndent(currentIndent)
                )
            }
        }

        impl(constructorCall) {
            additionalImports(ArbitraryImportable("org.jetbrains.kotlin.ir.util", "parentAsClass"))
            undefinedOffset()
            implementation.generationCallback = {
                println()
                print()
                println(
                    """
                companion object {
                    // Temporary API for compatible-compose, to be removed soon.
                    // Note: It cannot be marked with @Deprecated, because some usages in kotlin compiler pick this declaration up while it still exists.
                    fun fromSymbolOwner(
                        type: IrType,
                        constructorSymbol: IrConstructorSymbol,
                        origin: IrStatementOrigin? = null
                    ): IrConstructorCallImpl =
                        fromSymbolOwner(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, constructorSymbol, constructorSymbol.owner.parentAsClass.typeParameters.size,
                            origin
                        )
                }
                """.replaceIndent(currentIndent)
                )
            }
        }

        impl(delegatingConstructorCall) {
            implementation.generationCallback = {
                println()
                println("companion object")
            }
        }

        impl(enumConstructorCall) {
            implementation.generationCallback = {
                println()
                println("companion object")
            }
        }

        impl(functionReference) {
            implementation.generationCallback = {
                println()
                println("companion object")
            }
        }

        impl(const) {
            additionalImports(
                ArbitraryImportable("org.jetbrains.kotlin.builtins", "PrimitiveType"),
                ArbitraryImportable("org.jetbrains.kotlin.ir.expressions", "IrConstKind"),
                ArbitraryImportable("org.jetbrains.kotlin.ir.types", "getPrimitiveType"),
                ArbitraryImportable("org.jetbrains.kotlin.ir.types", "makeNullable"),
                ArbitraryImportable("org.jetbrains.kotlin.ir.types", "isMarkedNullable"),
            )
            implementation.generationCallback = {
                println()
                print()
                println(
                    """
                    companion object {
                        fun string(startOffset: Int, endOffset: Int, type: IrType, value: String): IrConstImpl<String> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.String, value)
                
                        fun int(startOffset: Int, endOffset: Int, type: IrType, value: Int): IrConstImpl<Int> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, value)
                
                        fun constNull(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<Nothing?> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Null, null)
                
                        fun boolean(startOffset: Int, endOffset: Int, type: IrType, value: Boolean): IrConstImpl<Boolean> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Boolean, value)
                
                        fun constTrue(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<Boolean> =
                            boolean(startOffset, endOffset, type, true)
                
                        fun constFalse(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<Boolean> =
                            boolean(startOffset, endOffset, type, false)
                
                        fun long(startOffset: Int, endOffset: Int, type: IrType, value: Long): IrConstImpl<Long> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, value)
                
                        fun float(startOffset: Int, endOffset: Int, type: IrType, value: Float): IrConstImpl<Float> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Float, value)
                
                        fun double(startOffset: Int, endOffset: Int, type: IrType, value: Double): IrConstImpl<Double> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Double, value)
                
                        fun char(startOffset: Int, endOffset: Int, type: IrType, value: Char): IrConstImpl<Char> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, value)
                
                        fun byte(startOffset: Int, endOffset: Int, type: IrType, value: Byte): IrConstImpl<Byte> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Byte, value)
                
                        fun short(startOffset: Int, endOffset: Int, type: IrType, value: Short): IrConstImpl<Short> =
                            IrConstImpl(startOffset, endOffset, type, IrConstKind.Short, value)
                
                        fun defaultValueForType(startOffset: Int, endOffset: Int, type: IrType): IrConstImpl<*> {
                            if (type.isMarkedNullable()) return constNull(startOffset, endOffset, type)
                            return when (type.getPrimitiveType()) {
                                PrimitiveType.BOOLEAN -> boolean(startOffset, endOffset, type, false)
                                PrimitiveType.CHAR -> char(startOffset, endOffset, type, 0.toChar())
                                PrimitiveType.BYTE -> byte(startOffset, endOffset, type, 0)
                                PrimitiveType.SHORT -> short(startOffset, endOffset, type, 0)
                                PrimitiveType.INT -> int(startOffset, endOffset, type, 0)
                                PrimitiveType.FLOAT -> float(startOffset, endOffset, type, 0.0F)
                                PrimitiveType.LONG -> long(startOffset, endOffset, type, 0)
                                PrimitiveType.DOUBLE -> double(startOffset, endOffset, type, 0.0)
                                else -> constNull(startOffset, endOffset, type.makeNullable())
                            }
                        }
                    }
                """.replaceIndent(currentIndent)
                )
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

        for (element in model.elements) {
            for (implementation in element.implementations) {
                if (element.category == Element.Category.Expression) {
                    implementation.isConstructorPublic = false
                }
            }
        }

        for (element in model.elements) {
            for (implementation in element.implementations) {
                if (element.category == Element.Category.Declaration) {
                    for (field in implementation.allFields) {
                        if (!field.name.let { it == "startOffset" || it == "endOffset"  || it == "origin" || it == "symbol" }) {
                            field.allowHoistingToBaseClass = false
                        }
                    }
                }
            }
        }
    }
}