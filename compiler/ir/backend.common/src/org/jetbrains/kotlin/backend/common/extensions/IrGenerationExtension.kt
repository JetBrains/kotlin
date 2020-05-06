/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope

// TODO: Make IrPluginContext be interface
open class IrPluginContext(
    @Deprecated("FrontEnd API shouldn't be accessed in Ir plugin environment")
    val moduleDescriptor: ModuleDescriptor,
    @Deprecated("FrontEnd API shouldn't be accessed in Ir plugin environment")
    val bindingContext: BindingContext,
    val languageVersionSettings: LanguageVersionSettings,
    @Deprecated("FrontEnd API shouldn't be accessed in Ir plugin environment")
    val symbolTable: ReferenceSymbolTable,
    @Deprecated("FrontEnd API shouldn't be accessed in Ir plugin environment")
    val typeTranslator: TypeTranslator,
    override val irBuiltIns: IrBuiltIns,
    private val linker: IrDeserializer,
    val symbols: BuiltinSymbolsBase = BuiltinSymbolsBase(irBuiltIns, irBuiltIns.builtIns, symbolTable)
) : IrGeneratorContext() {
    private fun resolveMemberScope(fqName: FqName): MemberScope? {
        val pkg = moduleDescriptor.getPackage(fqName)

        if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

        val parentMemberScope = resolveMemberScope(fqName.parent()) ?: return null

        val classDescriptor =
            parentMemberScope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND) as? ClassDescriptor ?: return null

        return classDescriptor.unsubstitutedMemberScope
    }

    private fun <S : IrSymbol> resolveSymbol(fqName: FqName, referencer: (MemberScope) -> S?): S? {
        val memberScope = resolveMemberScope(fqName) ?: return null

        val symbol = referencer(memberScope) ?: return null
        if (symbol.isBound) return symbol

        linker.getDeclaration(symbol)
        linker.postProcess()

        return symbol
    }

    private fun <S : IrSymbol> resolveSymbolCollection(fqName: FqName, referencer: (MemberScope) -> Collection<S>): Collection<S> {
        val memberScope = resolveMemberScope(fqName) ?: return emptyList()

        val symbols = referencer(memberScope)

        symbols.forEach { if (!it.isBound) linker.getDeclaration(it) }

        linker.postProcess()

        return symbols
    }

    fun referenceClass(fqName: FqName): IrClassSymbol? {
        assert(!fqName.isRoot)
        return resolveSymbol(fqName.parent()) { scope ->
            val classDescriptor = scope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND) as? ClassDescriptor?
            classDescriptor?.let {
                symbolTable.referenceClass(it)
            }
        }
    }

    fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> {
        val classSymbol = referenceClass(classFqn) ?: error("Cannot find class $classFqn")
        return classSymbol.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol }
    }

    fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> {
        assert(!fqName.isRoot)
        return resolveSymbolCollection(fqName.parent()) { scope ->
            val descriptors = scope.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            descriptors.map { symbolTable.referenceSimpleFunction(it) }
        }
    }

    fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> {
        assert(!fqName.isRoot)
        return resolveSymbolCollection(fqName.parent()) { scope ->
            val descriptors = scope.getContributedVariables(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            descriptors.map { symbolTable.referenceProperty(it) }
        }
    }
}

interface IrGenerationExtension {
    companion object :
        ProjectExtensionDescriptor<IrGenerationExtension>("org.jetbrains.kotlin.irGenerationExtension", IrGenerationExtension::class.java)

    fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    )
}

// Extension point for plugins which run before any lowerings, but after the Ir has been constructed.
@Deprecated("This is a temporary class which will be replaced with another extension mechanism soon.", level = DeprecationLevel.ERROR)
interface PureIrGenerationExtension {
    @Suppress("DEPRECATION_ERROR")
    companion object :
        ProjectExtensionDescriptor<PureIrGenerationExtension>(
            "org.jetbrains.kotlin.pureIrGenerationExtension", PureIrGenerationExtension::class.java
        )

    fun generate(
        moduleFragment: IrModuleFragment,
        context: CommonBackendContext
    )
}
