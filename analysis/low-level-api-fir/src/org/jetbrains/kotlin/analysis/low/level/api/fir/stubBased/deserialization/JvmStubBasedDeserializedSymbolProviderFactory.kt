/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.JvmFirDeserializedSymbolProviderFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.createJavaSymbolProvider
import org.jetbrains.kotlin.analysis.providers.createDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.fir.java.FirJavaFacade
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

class JvmStubBasedDeserializedSymbolProviderFactory : JvmFirDeserializedSymbolProviderFactory() {
    override fun createJvmFirDeserializedSymbolProviders(
        project: Project,
        session: FirSession,
        moduleData: LLFirModuleData,
        kotlinScopeProvider: FirKotlinScopeProvider,
        moduleDataProvider: SingleModuleDataProvider,
        firJavaFacade: FirJavaFacade,
        packagePartProvider: PackagePartProvider,
        scope: GlobalSearchScope
    ): List<FirSymbolProvider> {
        return buildList {
            //stub based provider here works over kotlin-only indices and thus provides only kotlin declarations
            //in order to find java declarations, one need to explicitly setup java symbol provider.
            //for ProtoBuf based provider (used in compiler), there is no need in separated java provider, 
            //because all declarations are retrieved at once and are not distinguished
            add(
                JvmStubBasedFirDeserializedSymbolProvider(
                    session,
                    moduleDataProvider,
                    kotlinScopeProvider,
                    project.createDeclarationProvider(object : DelegatingGlobalSearchScope(project, scope) {
                        override fun contains(file: VirtualFile): Boolean {
                            if (file.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) return false
                            return super.contains(file)
                        }
                    })
                )
            )
            add(createJavaSymbolProvider(session, moduleData, project, scope))
        }
    }
}