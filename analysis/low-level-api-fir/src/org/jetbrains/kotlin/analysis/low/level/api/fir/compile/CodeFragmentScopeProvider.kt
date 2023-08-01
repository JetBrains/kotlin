/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compile

import com.intellij.psi.impl.compiled.ClsTypeElementImpl
import com.intellij.psi.impl.compiled.SignatureParsing
import com.intellij.psi.impl.compiled.StubBuildingVisitor
import org.jetbrains.kotlin.analysis.providers.ForeignValueProviderService
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.java.JavaTypeParameterStack
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.jvm.buildJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.impl.JavaTypeImpl
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.org.objectweb.asm.Type
import java.text.StringCharacterIterator

val FirSession.codeFragmentScopeProvider: CodeFragmentScopeProvider by FirSession.sessionComponentAccessor()

private object ForeignValueMarkerDataKey : FirDeclarationDataKey()

var FirProperty.foreignValueMarker: Boolean? by FirDeclarationDataRegistry.data(ForeignValueMarkerDataKey)

class CodeFragmentScopeProvider(private val session: FirSession) : FirSessionComponent {
    private val foreignValueProvider = ForeignValueProviderService.getInstance()

    private val typeCache = session.firCachesFactory.createCache<String, FirTypeRef, KtCodeFragment> { typeDescriptor, ktCodeFragment ->
        getPrimitiveType(typeDescriptor, session)?.let { return@createCache it }

        val project = ktCodeFragment.project
        val javaElementSourceFactory = JavaElementSourceFactory.getInstance(project)

        val signatureIterator = StringCharacterIterator(typeDescriptor)
        val typeString = SignatureParsing.parseTypeString(signatureIterator, StubBuildingVisitor.GUESSING_MAPPER)
        val psiType = ClsTypeElementImpl(ktCodeFragment, typeString, '\u0000').type
        val javaType = JavaTypeImpl.create(psiType, javaElementSourceFactory.createTypeSource(psiType))

        val javaTypeRef = buildJavaTypeRef {
            annotationBuilder = { emptyList() }
            type = javaType
        }

        javaTypeRef.resolveIfJavaType(session, JavaTypeParameterStack.EMPTY)
    }

    fun getExtraScopes(codeFragment: KtCodeFragment): List<FirLocalScope> {
        val foreignValues = foreignValueProvider?.getForeignValues(codeFragment)?.takeUnless { it.isEmpty() } ?: return emptyList()
        return listOf(getForeignValuesScope(codeFragment, foreignValues))
    }

    private fun getForeignValuesScope(ktCodeFragment: KtCodeFragment, foreignValues: Map<String, String>): FirLocalScope {
        var result = FirLocalScope(session)

        for ((variableNameString, typeDescriptor) in foreignValues) {
            val variableName = Name.identifier(variableNameString)

            val variable = buildProperty {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = FirDeclarationOrigin.Source
                status = FirResolvedDeclarationStatusImpl(Visibilities.Local, Modality.FINAL, EffectiveVisibility.Local)
                returnTypeRef = typeCache.getValue(typeDescriptor, ktCodeFragment)
                deprecationsProvider = EmptyDeprecationsProvider
                name = variableName
                isVar = false
                symbol = FirPropertySymbol(variableName)
                isLocal = true
            }

            variable.foreignValueMarker = true

            result = result.storeVariable(variable, session)
        }

        return result
    }
}

private fun getPrimitiveType(typeDescriptor: String, session: FirSession): FirTypeRef? {
    val asmType = Type.getType(typeDescriptor)
    return when (asmType.sort) {
        Type.VOID -> session.builtinTypes.unitType
        Type.BOOLEAN -> session.builtinTypes.booleanType
        Type.CHAR -> session.builtinTypes.charType
        Type.BYTE -> session.builtinTypes.byteType
        Type.SHORT -> session.builtinTypes.shortType
        Type.INT -> session.builtinTypes.intType
        Type.FLOAT -> session.builtinTypes.floatType
        Type.LONG -> session.builtinTypes.longType
        Type.DOUBLE -> session.builtinTypes.doubleType
        else -> null
    }
}