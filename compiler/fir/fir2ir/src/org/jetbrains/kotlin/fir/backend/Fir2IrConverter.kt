/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

object Fir2IrConverter {

    fun createModuleFragment(
        session: FirSession,
        firFiles: List<FirFile>,
        languageVersionSettings: LanguageVersionSettings,
        fakeOverrideMode: FakeOverrideMode = FakeOverrideMode.NORMAL
    ): Fir2IrResult {
        val moduleDescriptor = FirModuleDescriptor(session)
        val symbolTable = SymbolTable()
        val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
        val typeTranslator = TypeTranslator(symbolTable, languageVersionSettings, moduleDescriptor.builtIns)
        constantValueGenerator.typeTranslator = typeTranslator
        typeTranslator.constantValueGenerator = constantValueGenerator
        val builtIns = IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable)
        val sourceManager = PsiSourceManager()
        val fir2irTransformer = Fir2IrVisitor(session, moduleDescriptor, symbolTable, sourceManager, builtIns, fakeOverrideMode)
        val irFiles = mutableListOf<IrFile>()
        for (firFile in firFiles) {
            val irFile = firFile.accept(fir2irTransformer, null) as IrFile
            val fileEntry = sourceManager.getOrCreateFileEntry(firFile.psi as KtFile)
            sourceManager.putFileEntry(irFile, fileEntry)
            irFiles += irFile
        }

        val irModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns, irFiles)
        generateUnboundSymbolsAsDependencies(irModuleFragment, symbolTable, builtIns)
        return Fir2IrResult(irModuleFragment, symbolTable, sourceManager)
    }

    private fun generateUnboundSymbolsAsDependencies(
        irModule: IrModuleFragment,
        symbolTable: SymbolTable,
        builtIns: IrBuiltIns
    ) {
        // TODO: provide StubGeneratorExtensions for correct lazy stub IR generation on JVM
        ExternalDependenciesGenerator(symbolTable, generateTypicalIrProviderList(irModule.descriptor, builtIns, symbolTable))
            .generateUnboundSymbolsAsDependencies()
    }
}