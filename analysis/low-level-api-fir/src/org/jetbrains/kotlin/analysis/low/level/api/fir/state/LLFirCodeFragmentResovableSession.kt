/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.builder.BodyBuildingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.builder.buildFileAnnotationsContainer
import org.jetbrains.kotlin.fir.builder.buildPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.unsubstitutedScope
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.types.ConstantValueKind

internal class LabeledThis(val name: String?, val type: FirTypeRef)

internal class LLFirCodeFragmentResovableSession(
    ktModule: KtModule,
    useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolvableResolveSession(ktModule, useSiteSessionFactory) {
    override fun getModuleKind(module: KtModule): ModuleKind {
        return ModuleKind.RESOLVABLE_MODULE
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        TODO("Not yet implemented")
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        TODO("Not yet implemented")
    }

    override fun getOrBuildFirFor(element: KtElement): FirElement? {
        val moduleComponents = getModuleComponentsForElement(element)
        return (element as? KtFile)?.let { moduleComponents.cache.fileCached(it) { buildFirFileFor(element, moduleComponents) } }
    }

    private fun buildFirFileFor(codeFragment: KtFile, moduleComponents: LLFirModuleResolveComponents): FirFile {
        val bodyCodeFragment = codeFragment.findDescendantOfType<KtExpression> { it is KtBlockExpression }!!

        val builder = object : RawFirBuilder(
            moduleComponents.session,
            moduleComponents.scopeProvider,
            bodyBuildingMode = BodyBuildingMode.NORMAL
        ) {
            fun build() = object : Visitor() {
                override fun visitKtFile(file: KtFile, data: FirElement?): FirElement {
                    return buildFile {
                        symbol = FirFileSymbol()
                        source = file.toFirSourceElement()
                        moduleData = baseModuleData
                        origin = FirDeclarationOrigin.Source
                        name = file.name
                        sourceFile = KtPsiSourceFile(file)
                        sourceFileLinesMapping = KtPsiSourceFileLinesMapping(file)
                        packageDirective = buildPackageDirective {
                            packageFqName = FqName.ROOT
                            source = file.packageDirective?.toKtPsiSourceElement()
                        }
                        annotationsContainer = buildAnnotationContainerForFile(moduleData, "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
                        declarations += buildCodeFragment {
                            symbol = FirCodeFragmentSymbol()
                            source = file.toFirSourceElement()
                            moduleData = baseModuleData
                            origin = FirDeclarationOrigin.Source

                            for (importDirective in file.importDirectives) {
                                imports += buildImport {
                                    source = importDirective.toFirSourceElement()
                                    importedFqName = importDirective.importedFqName
                                    isAllUnder = importDirective.isAllUnder
                                    aliasName = importDirective.aliasName?.let { Name.identifier(it) }
                                    aliasSource = importDirective.alias?.nameIdentifier?.toFirSourceElement()
                                }
                            }
                            codeBlock = buildBlock {
                                val danglingExpression = super.convertElement(bodyCodeFragment, null)
                                when (danglingExpression) {
                                    is FirBlock -> statements.addAll(danglingExpression.statements)
                                    is FirStatement -> statements.add(danglingExpression)
                                    else -> TODO()
                                }
                            }
                        }
                    }
                }
            }.convertElement(codeFragment)
        }
        val firCodeFragment = builder.build()
        return firCodeFragment as FirFile
    }

    private fun FirFileBuilder.buildAnnotationContainerForFile(
        moduleData: FirModuleData,
        vararg diagnostics: String
    ): FirFileAnnotationsContainer {
        return buildFileAnnotationsContainer {
            this.moduleData = moduleData
            containingFileSymbol = this@buildAnnotationContainerForFile.symbol
            /**
             * applying Suppress("INVISIBLE_*) to file, supposed to instruct frontend to ignore `private`
             * modifier.
             * TODO: investigate why it's not enough for
             * [org.jetbrains.kotlin.idea.k2.debugger.test.cases.K2EvaluateExpressionTestGenerated.SingleBreakpoint.CompilingEvaluator.InaccessibleMembers]
             */
            annotations += buildAnnotationCall {
                val annotationClassIdLookupTag = ClassId(
                    StandardNames.FqNames.suppress.parent(),
                    StandardNames.FqNames.suppress.shortName()
                ).toLookupTag()
                val annotationType = ConeClassLikeTypeImpl(
                    annotationClassIdLookupTag,
                    emptyArray(),
                    isNullable = false
                )
                calleeReference = buildResolvedNamedReference {
                    val annotationTypeSymbol = (annotationType.toSymbol(useSiteFirSession) as? FirRegularClassSymbol)
                        ?: return@buildAnnotationCall

                    val constructorSymbol =
                        annotationTypeSymbol.unsubstitutedScope(
                            useSiteFirSession,
                            useSiteFirSession.getScopeSession(),
                            withForcedTypeCalculator = false,
                            memberRequiredPhase = null
                        )
                            .getDeclaredConstructors().firstOrNull() ?: return@buildAnnotationCall
                    resolvedSymbol = constructorSymbol
                    name = constructorSymbol.name
                }
                argumentList = buildArgumentList {
                    arguments += buildVarargArgumentsExpression {
                        initialiazeSuppressAnnotionArguments(*diagnostics)
                    }
                }
                useSiteTarget = AnnotationUseSiteTarget.FILE
                annotationTypeRef = buildResolvedTypeRef {
                    type = annotationType
                }
                argumentMapping = buildAnnotationArgumentMapping {
                    mapping[Name.identifier("names")] = buildVarargArgumentsExpression {
                        initialiazeSuppressAnnotionArguments(*diagnostics)
                    }
                }
                annotationResolvePhase = FirAnnotationResolvePhase.Types
            }
        }
    }

    private fun FirVarargArgumentsExpressionBuilder.initialiazeSuppressAnnotionArguments(vararg diagnostics: String) {
        varargElementType =
            this@LLFirCodeFragmentResovableSession.useSiteFirSession.builtinTypes.stringType
        diagnostics.forEach {
            arguments += buildConstExpression(
                null,
                ConstantValueKind.String,
                it
            )
        }
    }
}