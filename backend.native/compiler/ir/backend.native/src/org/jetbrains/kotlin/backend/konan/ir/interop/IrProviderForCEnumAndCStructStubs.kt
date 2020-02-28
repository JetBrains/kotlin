/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop

import org.jetbrains.kotlin.backend.konan.InteropBuiltIns
import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumByValueFunctionGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumClassGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumCompanionGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumVarClassGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cstruct.CStructVarClassGenerator
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

/**
 * For the most of descriptors that come from metadata-based interop libraries
 * we generate a lazy IR.
 * We use a different approach for CEnums and CStructVars and generate IR eagerly. Motivation:
 * 1. CEnums are "real" Kotlin enums. Thus, we need apply the same compilation approach
 *   as we use for usual Kotlin enums.
 *   Eager generation allows to reuse [EnumClassLowering], [EnumConstructorsLowering] and other
 *   compiler phases.
 * 2. It is an easier and more obvious approach. Since implementation of metadata-based
 *  libraries generation already took too much time we take an easier approach here.
 */
internal class IrProviderForCEnumAndCStructStubs(
        context: GeneratorContext,
        private val interopBuiltIns: InteropBuiltIns,
        symbols: KonanSymbols
) : IrProvider {

    private val symbolTable: SymbolTable = context.symbolTable

    private val filesMap = mutableMapOf<PackageFragmentDescriptor, IrFile>()

    private val cEnumByValueFunctionGenerator =
            CEnumByValueFunctionGenerator(context, symbols)
    private val cEnumCompanionGenerator =
            CEnumCompanionGenerator(context, cEnumByValueFunctionGenerator)
    private val cEnumVarClassGenerator =
            CEnumVarClassGenerator(context, interopBuiltIns)
    private val cEnumClassGenerator =
            CEnumClassGenerator(context, cEnumCompanionGenerator, cEnumVarClassGenerator)
    private val cStructClassGenerator =
            CStructVarClassGenerator(context, interopBuiltIns)

    var module: IrModuleFragment? = null
        set(value) {
            if (value == null)
                error("Provide a valid non-null module")
            if (field != null)
                error("Module has already been set")
            field = value
            value.files += filesMap.values
        }

    fun canHandleSymbol(symbol: IrSymbol): Boolean {
        if (!symbol.isPublicApi) return false
        if (symbol.signature.run { !IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test() }) return false
        return symbol.findCEnumDescriptor(interopBuiltIns) != null
                || symbol.findCStructDescriptor(interopBuiltIns) != null
    }

    fun buildAllEnumsAndStructsFrom(interopModule: ModuleDescriptor) = interopModule.getPackageFragments()
            .flatMap { it.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) }
            .filterIsInstance<ClassDescriptor>()
            .forEach {
                when {
                    it.implementsCEnum(interopBuiltIns) ->
                        cEnumClassGenerator.findOrGenerateCEnum(it, irParentFor(it))
                    it.inheritsFromCStructVar(interopBuiltIns) ->
                        cStructClassGenerator.findOrGenerateCStruct(it, irParentFor(it))
                }
            }

    private fun generateIrIfNeeded(symbol: IrSymbol) {
        // TODO: These `findOrGenerate` calls generate a whole subtree.
        //  This a simple but clearly suboptimal solution.
        symbol.findCEnumDescriptor(interopBuiltIns)?.let { enumDescriptor ->
            cEnumClassGenerator.findOrGenerateCEnum(enumDescriptor, irParentFor(enumDescriptor))
        }
        symbol.findCStructDescriptor(interopBuiltIns)?.let { structDescriptor ->
            cStructClassGenerator.findOrGenerateCStruct(structDescriptor, irParentFor(structDescriptor))
        }
    }

    override fun getDeclaration(symbol: IrSymbol): IrDeclaration? {
        if (symbol.isBound) return symbol.owner as IrDeclaration
        if (!canHandleSymbol(symbol)) return null
        generateIrIfNeeded(symbol)
        return when (symbol) {
            is IrClassSymbol -> symbolTable.referenceClass(symbol.descriptor).owner
            is IrEnumEntrySymbol -> symbolTable.referenceEnumEntry(symbol.descriptor).owner
            is IrFunctionSymbol -> symbolTable.referenceFunction(symbol.descriptor).owner
            is IrPropertySymbol -> symbolTable.referenceProperty(symbol.descriptor).owner
            else -> error(symbol)
        }
    }

    private fun irParentFor(descriptor: ClassDescriptor): IrDeclarationContainer {
        val packageFragmentDescriptor = descriptor.findPackage()
        return filesMap.getOrPut(packageFragmentDescriptor) {
            IrFileImpl(NaiveSourceBasedFileEntryImpl(cTypeDefinitionsFileName), packageFragmentDescriptor).also {
                this@IrProviderForCEnumAndCStructStubs.module?.files?.add(it)
            }
        }
    }

    companion object {
        const val cTypeDefinitionsFileName = "CTypeDefinitions"
    }
}