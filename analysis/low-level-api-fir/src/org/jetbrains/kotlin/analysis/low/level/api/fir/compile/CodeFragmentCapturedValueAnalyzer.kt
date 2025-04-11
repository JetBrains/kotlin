/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compile

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingKtFileIfAny
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.parentsCodeFragmentAware
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.referencedMemberSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import java.util.*

@KaImplementationDetail
class CodeFragmentCapturedSymbol(
    val value: CodeFragmentCapturedValue,
    val symbol: FirBasedSymbol<*>,
    val typeRef: FirTypeRef,
)

@KaImplementationDetail
data class CodeFragmentCapturedId(val symbol: FirBasedSymbol<*>)

@KaImplementationDetail
object CodeFragmentCapturedValueAnalyzer {
    fun analyze(resolutionFacade: LLResolutionFacade, codeFragment: FirCodeFragment): CodeFragmentCapturedValueData {
        val selfSymbols = CodeFragmentDeclarationCollector().apply { codeFragment.accept(this) }.symbols.toSet()
        val capturedVisitor = CodeFragmentCapturedValueVisitor(resolutionFacade, selfSymbols)
        codeFragment.accept(capturedVisitor)
        return CodeFragmentCapturedValueData(capturedVisitor.values, capturedVisitor.files, capturedVisitor.reifiedTypeParameters)
    }
}

@KaImplementationDetail
class CodeFragmentCapturedValueData(
    val symbols: List<CodeFragmentCapturedSymbol>,
    val files: List<KtFile>,
    val reifiedTypeParameters: Set<FirTypeParameterSymbol>,
)

private class CodeFragmentDeclarationCollector : FirDefaultVisitorVoid() {
    private val collectedSymbols = mutableListOf<FirBasedSymbol<*>>()

    val symbols: List<FirBasedSymbol<*>>
        get() = Collections.unmodifiableList(collectedSymbols)

    override fun visitElement(element: FirElement) {
        if (element is FirDeclaration) {
            collectedSymbols += element.symbol
        }

        element.acceptChildren(this)
    }
}

private class CodeFragmentCapturedValueVisitor(
    private val resolutionFacade: LLResolutionFacade,
    private val selfSymbols: Set<FirBasedSymbol<*>>,
) : FirDefaultVisitorVoid() {
    private val collectedMappings = LinkedHashMap<CodeFragmentCapturedId, CodeFragmentCapturedSymbol>()
    private val collectedFiles = LinkedHashSet<KtFile>()
    private val collectedReifiedTypeParameters = HashSet<FirTypeParameterSymbol>()

    private val assignmentLhs = mutableListOf<FirBasedSymbol<*>>()

    val values: List<CodeFragmentCapturedSymbol>
        get() = collectedMappings.values.toList()

    val files: List<KtFile>
        get() = collectedFiles.toList()

    val reifiedTypeParameters: Set<FirTypeParameterSymbol>
        get() = collectedReifiedTypeParameters.toSet()

    private val session: FirSession
        get() = resolutionFacade.useSiteFirSession

    override fun visitElement(element: FirElement) {
        processElement(element)

        val lhs = (element as? FirVariableAssignment)?.lValue?.toResolvedCallableSymbol(session)
        if (lhs != null) {
            assignmentLhs.add(lhs)
        }

        element.acceptChildren(this)

        if (lhs != null) {
            require(assignmentLhs.removeLast() == lhs)
        }
    }

    private fun processElement(element: FirElement) {
        if (element is FirExpression) {
            val symbol = element.resolvedType.toSymbol(session)
            if (symbol != null) {
                registerFileIfRequired(symbol)
            }
        }

        when (element) {
            is FirSuperReference -> {
                val symbol = (element.superTypeRef as? FirResolvedTypeRef)?.toRegularClassSymbol(session)
                if (symbol != null && symbol !in selfSymbols) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = CodeFragmentCapturedValue.SuperClass(symbol.classId, isCrossingInlineBounds)
                    register(CodeFragmentCapturedSymbol(capturedValue, symbol, element.superTypeRef))
                }
            }
            is FirThisReference -> {
                val symbol = element.boundSymbol
                if (symbol != null && (symbol as FirBasedSymbol<*>?) !in selfSymbols) {
                    fun registerClassSymbolIfNotObject(classSymbol: FirClassSymbol<*>) {
                        if (classSymbol.classKind != ClassKind.OBJECT) {
                            val isCrossingInlineBounds = isCrossingInlineBounds(element, classSymbol)
                            val capturedValue = CodeFragmentCapturedValue.ContainingClass(classSymbol.classId, isCrossingInlineBounds)
                            val typeRef = buildResolvedTypeRef { coneType = classSymbol.defaultType() }
                            register(CodeFragmentCapturedSymbol(capturedValue, classSymbol, typeRef))
                        }
                    }

                    when (symbol) {
                        is FirClassSymbol<*> -> {
                            registerClassSymbolIfNotObject(symbol)
                        }
                        // TODO(KT-72994) remove branch when context receivers are removed
                        is FirValueParameterSymbol -> {
                            val valueParameter = symbol.fir
                            val referencedSymbol = element.referencedMemberSymbol
                            if (referencedSymbol is FirClassSymbol) {
                                // Specific (deprecated) case for a class context receiver
                                registerClassSymbolIfNotObject(referencedSymbol)
                            } else {
                                val labelName = valueParameter.name
                                if (labelName != SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) {
                                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                    val index = when (val containingDeclaration = symbol.containingDeclarationSymbol.fir) {
                                        is FirCallableDeclaration -> containingDeclaration.contextParameters.indexOf(
                                            valueParameter
                                        )
                                        is FirRegularClass -> containingDeclaration.contextParameters.indexOf(valueParameter)
                                        else -> errorWithFirSpecificEntries(
                                            message = "Unexpected containing declaration ${containingDeclaration::class.simpleName}",
                                            fir = containingDeclaration
                                        )
                                    }
                                    val capturedValue = CodeFragmentCapturedValue
                                        .ContextReceiver(index, labelName, isCrossingInlineBounds)
                                    register(
                                        CodeFragmentCapturedSymbol(
                                            capturedValue,
                                            symbol,
                                            valueParameter.returnTypeRef
                                        )
                                    )
                                }
                            }
                        }
                        is FirReceiverParameterSymbol -> {
                            val receiverParameter = symbol.fir
                            val labelName = element.labelName
                                ?: (receiverParameter.containingDeclarationSymbol as? FirAnonymousFunctionSymbol)?.label?.name
                                ?: (receiverParameter.containingDeclarationSymbol as FirCallableSymbol).name.asString()

                            val typeRef = receiverParameter.typeRef
                            val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                            val capturedValue = CodeFragmentCapturedValue.ExtensionReceiver(labelName, isCrossingInlineBounds)
                            register(
                                CodeFragmentCapturedSymbol(capturedValue, receiverParameter.symbol, typeRef)
                            )
                        }
                        is FirTypeAliasSymbol, is FirTypeParameterSymbol -> errorWithFirSpecificEntries(
                            message = "Unexpected FirThisOwnerSymbol ${symbol::class.simpleName}", fir = symbol.fir
                        )
                    }
                }
            }
            is FirResolvable -> {
                val symbol = element.calleeReference.toResolvedCallableSymbol()
                if (symbol != null && symbol !in selfSymbols) {
                    processCall(element, symbol)
                }
            }
            is FirResolvedTypeRef -> {
                processConeType(element.coneType)
            }
        }
    }

    private fun processConeType(type: ConeKotlinType) {
        if (type is ConeTypeParameterType) {
            val symbol = type.lookupTag.typeParameterSymbol
            if (symbol.isReified) collectedReifiedTypeParameters.add(symbol)
        }
        for (typeArgument in type.typeArguments) {
            typeArgument.type?.let { processConeType(it) }
        }
    }

    private fun processCall(element: FirElement, symbol: FirCallableSymbol<*>) {
        // Desugared inc/dec FIR looks as follows:
        // lval <unary>: R|kotlin/Int| = R|<local>/x|
        // R|<local>/x| = R|<local>/<unary>|.R|kotlin/Int.inc|()
        // We visit the x in the first line before we visit the assignment and need to check the source to determine that the variable
        // is mutated.
        // The x in the second line isn't visited because it's a FirDesugaredAssignmentValueReferenceExpression.
        val isMutated = assignmentLhs.lastOrNull() == symbol || element.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement
        when (symbol) {
            is FirValueParameterSymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CodeFragmentCapturedValue.Local(symbol.name, isMutated, isCrossingInlineBounds)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is FirPropertySymbol -> {
                if (symbol.isLocal) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = when {
                        symbol.isForeignValue -> CodeFragmentCapturedValue.ForeignValue(symbol.name, isCrossingInlineBounds)
                        symbol.hasDelegate -> CodeFragmentCapturedValue.LocalDelegate(symbol.name, isMutated, isCrossingInlineBounds)
                        else -> CodeFragmentCapturedValue.Local(symbol.name, isMutated, isCrossingInlineBounds)
                    }
                    register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
                } else {
                    // Property call generation depends on complete backing field resolution (Fir2IrLazyProperty.backingField)
                    symbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                    registerFileIfRequired(symbol)
                }
            }
            is FirBackingFieldSymbol -> {
                val propertyName = symbol.propertySymbol.name
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CodeFragmentCapturedValue.BackingField(propertyName, isMutated, isCrossingInlineBounds)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is FirNamedFunctionSymbol -> {
                registerFileIfRequired(symbol)
            }
        }

        if (symbol.callableId == StandardClassIds.Callables.coroutineContext) {
            val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
            val capturedValue = CodeFragmentCapturedValue.CoroutineContext(isCrossingInlineBounds)
            register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
        }
    }

    private fun register(mapping: CodeFragmentCapturedSymbol) {
        val id = CodeFragmentCapturedId(mapping.symbol)
        val previousMapping = collectedMappings[id]

        if (previousMapping != null) {
            val previousValue = previousMapping.value
            val newValue = mapping.value

            require(previousValue.javaClass == newValue.javaClass)

            // Only replace non-mutated value with a mutated one.
            if (previousValue.isMutated || !newValue.isMutated) {
                return
            }
        }

        collectedMappings[id] = mapping
        registerFileIfRequired(mapping.symbol)
    }

    private val FirFunctionSymbol<*>.isAnnotatedWithNonLiteralJvmName: Boolean
        get() {
            val jvmNameAnnotation = annotations.getAnnotationByClassId(StandardClassIds.Annotations.jvmName, session) ?: return false

            lazyResolveToPhase(FirResolvePhase.ANNOTATION_ARGUMENTS)

            val argument = jvmNameAnnotation.argumentMapping.mapping[Name.identifier("name")]
            return argument != null && argument !is FirLiteralExpression
        }

    private val FirFunctionSymbol<*>.hasAnnotationArgumentShouldBeEvaluated: Boolean
        get() {
            return isAnnotatedWithNonLiteralJvmName
        }

    private fun registerFileIfRequired(symbol: FirBasedSymbol<*>) {
        if (symbol is FirTypeParameterSymbol && symbol.isReified) collectedReifiedTypeParameters.add(symbol)

        val needsRegistration = when (symbol) {
            is FirRegularClassSymbol -> symbol.isLocal
            is FirAnonymousObjectSymbol -> true
            is FirNamedFunctionSymbol -> symbol.callableId.isLocal || symbol.hasAnnotationArgumentShouldBeEvaluated
            is FirPropertySymbol ->
                symbol.getterSymbol?.hasAnnotationArgumentShouldBeEvaluated == true
                        || symbol.setterSymbol?.hasAnnotationArgumentShouldBeEvaluated == true
            else -> false
        }

        if (!needsRegistration) {
            return
        }

        val file = symbol.fir.containingKtFileIfAny ?: return
        if (!file.isCompiled) {
            collectedFiles.add(file)
        }
    }

    private fun isCrossingInlineBounds(element: FirElement, symbol: FirBasedSymbol<*>): Boolean {
        val callSite = element.source?.psi ?: return false
        val declarationSite = symbol.fir.source?.psi ?: return false
        val commonParent = findCommonParentContextAware(callSite, declarationSite) ?: return false

        for (elementInBetween in callSite.parentsCodeFragmentAware) {
            if (elementInBetween === commonParent) {
                break
            }

            if (elementInBetween is KtFunction) {
                val symbolInBetween = elementInBetween.resolveToFirSymbol(resolutionFacade)
                if (symbolInBetween is FirCallableSymbol<*> && !symbolInBetween.isInline) {
                    return true
                }
            }
        }

        return false
    }

    private fun findCommonParentContextAware(callSite: PsiElement, declarationSite: PsiElement): PsiElement? {
        val directParent = PsiTreeUtil.findCommonParent(callSite, declarationSite)
        if (directParent != null) {
            return directParent
        }

        val codeFragment = callSite.containingFile as? KtCodeFragment ?: return null
        val codeFragmentContext = codeFragment.context ?: return null
        return PsiTreeUtil.findCommonParent(codeFragmentContext, declarationSite)
    }
}
