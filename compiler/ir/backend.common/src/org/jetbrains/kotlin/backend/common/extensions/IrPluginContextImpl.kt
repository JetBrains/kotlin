/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.extensions

import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope

open class IrPluginContextImpl constructor(
    private val module: ModuleDescriptor,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val bindingContext: BindingContext,
    override val languageVersionSettings: LanguageVersionSettings,
    private val st: ReferenceSymbolTable,
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val typeTranslator: TypeTranslator,
    override val irBuiltIns: IrBuiltIns,
    val linker: IrDeserializer,
    override val symbols: BuiltinSymbolsBase = BuiltinSymbolsBase(irBuiltIns, irBuiltIns.builtIns, st)
) : IrPluginContext {

    override val platform: TargetPlatform? = module.platform

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val moduleDescriptor: ModuleDescriptor = module
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val symbolTable: ReferenceSymbolTable = st

    private fun resolveMemberScope(fqName: FqName): MemberScope? {
        val pkg = module.getPackage(fqName)

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

    override fun referenceClass(fqName: FqName): IrClassSymbol? {
        assert(!fqName.isRoot)
        return resolveSymbol(fqName.parent()) { scope ->
            val classDescriptor = scope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND) as? ClassDescriptor?
            classDescriptor?.let {
                st.referenceClass(it)
            }
        }
    }

    override fun referenceTypeAlias(fqName: FqName): IrTypeAliasSymbol? {
        assert(!fqName.isRoot)
        return resolveSymbol(fqName.parent()) { scope ->
            val aliasDescriptor = scope.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND) as? TypeAliasDescriptor?
            aliasDescriptor?.let {
                st.referenceTypeAlias(it)
            }
        }
    }

    override fun referenceConstructors(classFqn: FqName): Collection<IrConstructorSymbol> {
        val classSymbol = referenceClass(classFqn) ?: error("Cannot find class $classFqn")
        return classSymbol.owner.declarations.filterIsInstance<IrConstructor>().map { it.symbol }
    }

    override fun referenceFunctions(fqName: FqName): Collection<IrSimpleFunctionSymbol> {
        assert(!fqName.isRoot)
        return resolveSymbolCollection(fqName.parent()) { scope ->
            val descriptors = scope.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            descriptors.map { st.referenceSimpleFunction(it) }
        }
    }

    override fun referenceProperties(fqName: FqName): Collection<IrPropertySymbol> {
        assert(!fqName.isRoot)
        return resolveSymbolCollection(fqName.parent()) { scope ->
            val descriptors = scope.getContributedVariables(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            descriptors.map { st.referenceProperty(it) }
        }
    }
}
