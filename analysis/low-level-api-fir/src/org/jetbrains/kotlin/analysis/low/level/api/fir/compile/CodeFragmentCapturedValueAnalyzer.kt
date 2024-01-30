/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compile

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.api.compile.CodeFragmentCapturedValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.containingKtFileIfAny
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.parentsCodeFragmentAware
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.labelName
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.toSymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import java.util.*

class CodeFragmentCapturedSymbol(
    val value: CodeFragmentCapturedValue,
    val symbol: FirBasedSymbol<*>,
    val typeRef: FirTypeRef,
    val contextReceiverNumber: Int = -1
)

data class CodeFragmentCapturedId(val symbol: FirBasedSymbol<*>, val contextReceiverNumber: Int = -1)

object CodeFragmentCapturedValueAnalyzer {
    fun analyze(resolveSession: LLFirResolveSession, codeFragment: FirCodeFragment): CodeFragmentCapturedValueData {
        val selfSymbols = CodeFragmentDeclarationCollector().apply { codeFragment.accept(this) }.symbols.toSet()
        val capturedVisitor = CodeFragmentCapturedValueVisitor(resolveSession, selfSymbols)
        codeFragment.accept(capturedVisitor)
        return CodeFragmentCapturedValueData(capturedVisitor.values, capturedVisitor.files)
    }
}

class CodeFragmentCapturedValueData(val symbols: List<CodeFragmentCapturedSymbol>, val files: List<KtFile>)

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
    private val resolveSession: LLFirResolveSession,
    private val selfSymbols: Set<FirBasedSymbol<*>>,
) : FirDefaultVisitorVoid() {
    private val collectedMappings = LinkedHashMap<CodeFragmentCapturedId, CodeFragmentCapturedSymbol>()
    private val collectedFiles = LinkedHashSet<KtFile>()

    private val assignmentLhs = mutableListOf<FirBasedSymbol<*>>()

    val values: List<CodeFragmentCapturedSymbol>
        get() = collectedMappings.values.toList()

    val files: List<KtFile>
        get() = collectedFiles.toList()

    private val session: FirSession
        get() = resolveSession.useSiteFirSession

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
                registerFile(symbol)
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
                val contextReceiverNumber = element.contextReceiverNumber
                val symbol = element.boundSymbol
                if (symbol != null && symbol !in selfSymbols) {
                    when (symbol) {
                        is FirClassSymbol<*> -> {
                            if (symbol.classKind != ClassKind.OBJECT) {
                                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                val capturedValue = CodeFragmentCapturedValue.ContainingClass(symbol.classId, isCrossingInlineBounds)
                                val typeRef = buildResolvedTypeRef { type = symbol.defaultType() }
                                register(CodeFragmentCapturedSymbol(capturedValue, symbol, typeRef, contextReceiverNumber))
                            }
                        }
                        is FirFunctionSymbol<*>, is FirPropertySymbol -> {
                            @Suppress("USELESS_IS_CHECK") // Smart-cast is not applied from a 'when' condition in K1
                            require(symbol is FirCallableSymbol<*>)

                            if (contextReceiverNumber >= 0) {
                                val contextReceiver = symbol.resolvedContextReceivers[contextReceiverNumber]
                                val labelName = contextReceiver.labelName
                                if (labelName != null) {
                                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                    val capturedValue = CodeFragmentCapturedValue
                                        .ContextReceiver(contextReceiverNumber, labelName, isCrossingInlineBounds)
                                    register(
                                        CodeFragmentCapturedSymbol(capturedValue, symbol, contextReceiver.typeRef, contextReceiverNumber)
                                    )
                                }
                            } else {
                                val labelName = element.labelName
                                    ?: (symbol as? FirAnonymousFunctionSymbol)?.label?.name
                                    ?: symbol.name.asString()

                                val typeRef = symbol.receiverParameter?.typeRef ?: error("Receiver parameter not found")
                                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                val capturedValue = CodeFragmentCapturedValue.ExtensionReceiver(labelName, isCrossingInlineBounds)
                                register(CodeFragmentCapturedSymbol(capturedValue, symbol, typeRef, contextReceiverNumber))
                            }
                        }
                    }
                }
            }
            is FirResolvable -> {
                val symbol = element.calleeReference.toResolvedCallableSymbol()
                if (symbol != null && symbol !in selfSymbols) {
                    processCall(element, symbol)
                }
            }
        }
    }

    private fun processCall(element: FirElement, symbol: FirCallableSymbol<*>) {
        when (symbol) {
            is FirValueParameterSymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CodeFragmentCapturedValue.Local(symbol.name, symbol.isMutated, isCrossingInlineBounds)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is FirPropertySymbol -> {
                if (symbol.isLocal) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = when {
                        symbol.fir.foreignValueMarker == true -> CodeFragmentCapturedValue.ForeignValue(symbol.name, isCrossingInlineBounds)
                        symbol.hasDelegate -> CodeFragmentCapturedValue.LocalDelegate(symbol.name, symbol.isMutated, isCrossingInlineBounds)
                        else -> CodeFragmentCapturedValue.Local(symbol.name, symbol.isMutated, isCrossingInlineBounds)
                    }
                    register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
                } else {
                    // Property call generation depends on complete backing field resolution (Fir2IrLazyProperty.backingField)
                    symbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                }
            }
            is FirBackingFieldSymbol -> {
                val propertyName = symbol.propertySymbol.name
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CodeFragmentCapturedValue.BackingField(propertyName, symbol.isMutated, isCrossingInlineBounds)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is FirNamedFunctionSymbol -> {
                if (symbol.isLocal) {
                    registerFile(symbol)
                }
            }
        }

        if (symbol.callableId == StandardClassIds.Callables.coroutineContext) {
            val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
            val capturedValue = CodeFragmentCapturedValue.CoroutineContext(isCrossingInlineBounds)
            register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
        }
    }

    private fun register(mapping: CodeFragmentCapturedSymbol) {
        val id = CodeFragmentCapturedId(mapping.symbol, mapping.contextReceiverNumber)
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
        registerFile(mapping.symbol)
    }

    private fun registerFile(symbol: FirBasedSymbol<*>) {
        val needsRegistration = when (symbol) {
            is FirRegularClassSymbol -> symbol.isLocal
            is FirAnonymousObjectSymbol -> true
            is FirNamedFunctionSymbol -> symbol.callableId.isLocal
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
                val symbolInBetween = elementInBetween.resolveToFirSymbol(resolveSession)
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

    private val FirBasedSymbol<*>.isMutated: Boolean
        get() = assignmentLhs.lastOrNull() == this
}
