/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.TowerDataKind.*
import org.jetbrains.kotlin.fir.resolve.transformQualifiedAccessUsingSmartcastInfo
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.firUnsafe
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction.NONE
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitSimpleImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirLocalScope
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeIntegerLiteralType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.descriptorUtil.HIDES_MEMBERS_NAME_LIST
import org.jetbrains.kotlin.util.OperatorNameConventions

enum class TowerDataKind {
    EMPTY,       // Corresponds to stub tower level which is replaced by receiver-related level
    TOWER_LEVEL  // Corresponds to real tower level which may process elements itself
}

class FirTowerResolver(
    val typeCalculator: ReturnTypeCalculator,
    val components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner,
    val topLevelScopes: List<FirScope>,
    val localScopes: List<FirLocalScope>
) {

    val session: FirSession get() = components.session

    private fun processImplicitReceiver(
        towerDataConsumer: TowerDataConsumer,
        implicitReceiverValue: ImplicitReceiverValue<*>,
        nonEmptyLocalScopes: List<FirLocalScope>,
        oldGroup: Int
    ): Int {
        var group = oldGroup
        // Member (no explicit receiver) / extension member (with explicit receiver) access via implicit receiver
        // class Foo(val x: Int) {
        //     fun Bar.baz() {}
        //     fun test() { x }
        //     fun test(b: Bar) { b.baz() }
        // }
        towerDataConsumer.consume(
            TOWER_LEVEL,
            MemberScopeTowerLevel(session, components, dispatchReceiver = implicitReceiverValue, scopeSession = components.scopeSession),
            group++
        )

        // class Foo {
        //     fun foo(block: Foo.() -> Unit) {
        //         block()
        //     }
        // }
        // invokeExtension on local variable
        towerDataConsumer.consume(
            EMPTY,
            TowerScopeLevel.OnlyImplicitReceiver(implicitReceiverValue),
            group++
        )
        //TowerData.OnlyImplicitReceiver(implicitReceiver).process()?.let { return it }

        // Same receiver is dispatch & extension
//        class Foo {
//            fun Foo.bar() {}
//            fun test() { bar() }
//        }
        towerDataConsumer.consume(
            TOWER_LEVEL,
            MemberScopeTowerLevel(
                session, components,
                dispatchReceiver = implicitReceiverValue,
                implicitExtensionReceiver = implicitReceiverValue,
                scopeSession = components.scopeSession
            ),
            group++
        )

        for (scope in nonEmptyLocalScopes) {
            // Local scope extensions via implicit receiver
            // class Foo {
            //     fun test() {
            //         fun Foo.bar() {}
            //         bar()
            //     }
            // }
            towerDataConsumer.consume(
                TOWER_LEVEL,
                ScopeTowerLevel(session, components, scope, implicitExtensionReceiver = implicitReceiverValue),
                group++
            )
        }

        var blockDispatchReceivers = false

        for (implicitDispatchReceiverValue in implicitReceiverValues) {
            val implicitScope = implicitDispatchReceiverValue.implicitScope
            if (implicitScope != null) {
                // Extensions in outer object
                //  object Outer {
                //     fun Nested.foo() {}
                //     class Nested {
                //         fun test() { foo() }
                //     }
                // }
                towerDataConsumer.consume(
                    TOWER_LEVEL,
                    ScopeTowerLevel(session, components, implicitScope, implicitExtensionReceiver = implicitReceiverValue),
                    group++
                )
            }
            if (implicitDispatchReceiverValue is ImplicitDispatchReceiverValue) {
                val implicitCompanionScopes = implicitDispatchReceiverValue.implicitCompanionScopes
                for (implicitCompanionScope in implicitCompanionScopes) {
                    // Extension in companion
                    // class My {
                    //     companion object { fun My.foo() {} }
                    //     fun test() { foo() }
                    // }
                    towerDataConsumer.consume(
                        TOWER_LEVEL,
                        ScopeTowerLevel(session, components, implicitCompanionScope, implicitExtensionReceiver = implicitReceiverValue),
                        group++
                    )
                }
                if (blockDispatchReceivers) {
                    continue
                }
                if ((implicitDispatchReceiverValue.boundSymbol.fir as? FirRegularClass)?.isInner == false) {
                    blockDispatchReceivers = true
                }
            }
            if (implicitDispatchReceiverValue !== implicitReceiverValue) {
                // Two different implicit receivers (dispatch & extension)
                // class A
                // class B {
                //     fun A.foo() {}
                // }
                // fun test(a: A, b: B) {
                //     with(a) { with(b) { foo() } }
                // }
                towerDataConsumer.consume(
                    TOWER_LEVEL,
                    MemberScopeTowerLevel(
                        session, components,
                        scopeSession = components.scopeSession,
                        dispatchReceiver = implicitDispatchReceiverValue,
                        implicitExtensionReceiver = implicitReceiverValue
                    ),
                    group++
                )
            }
        }

        return group
    }

    private fun processTopLevelScope(
        towerDataConsumer: TowerDataConsumer,
        topLevelScope: FirScope,
        oldGroup: Int,
        extensionsOnly: Boolean = false
    ): Int {
        var group = oldGroup
        // Top-level extensions via implicit receiver
        // fun Foo.bar() {}
        // class Foo {
        //     fun test() { bar() }
        // }
        for (implicitReceiverValue in implicitReceiverValues) {
            if (towerDataConsumer.consume(
                    TOWER_LEVEL,
                    ScopeTowerLevel(
                        session, components, topLevelScope,
                        implicitExtensionReceiver = implicitReceiverValue,
                        extensionsOnly = extensionsOnly
                    ),
                    group++
                ) == NONE
            ) {
                return group
            }
        }
        // Member of top-level scope & importing scope
        // val x = 0
        // fun test() { x }
        towerDataConsumer.consume(TOWER_LEVEL, ScopeTowerLevel(session, components, topLevelScope), group++)
        return group
    }

    fun reset() {
        collector.newDataSet()
    }

    val collector = CandidateCollector(components, resolutionStageRunner)
    private lateinit var towerDataConsumer: TowerDataConsumer
    private lateinit var implicitReceiverValues: List<ImplicitReceiverValue<*>>

    private fun towerDataConsumer(info: CallInfo, collector: CandidateCollector): TowerDataConsumer {
        return when (info.callKind) {
            CallKind.VariableAccess -> {
                createVariableAndObjectConsumer(session, info.name, info, components, collector)
            }
            CallKind.Function -> {
                createFunctionConsumer(session, info.name, info, components, collector, this)
            }
            CallKind.CallableReference -> {
                if (info.stubReceiver == null) {
                    createCallableReferencesConsumer(session, info.name, info, components, collector)
                } else {
                    PrioritizedTowerDataConsumer(
                        collector,
                        createCallableReferencesConsumer(
                            session, info.name, info.replaceExplicitReceiver(info.stubReceiver), components, collector
                        ),
                        createCallableReferencesConsumer(
                            session, info.name, info, components, collector
                        )
                    )
                }
            }
            else -> throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
        }
    }

    private fun createExplicitReceiverForInvoke(candidate: Candidate): FirQualifiedAccessExpressionImpl {
        val symbol = candidate.symbol as FirCallableSymbol<*>
        return FirQualifiedAccessExpressionImpl(null).apply {
            calleeReference = FirNamedReferenceWithCandidate(
                null,
                symbol.callableId.callableName,
                candidate
            )
            dispatchReceiver = candidate.dispatchReceiverExpression()
            typeRef = typeCalculator.tryCalculateReturnType(symbol.firUnsafe())
        }
    }

    // Only case when qualifiedReceiver is a package (no classId) is handled here
    private fun runResolverForFullyQualifiedReceiver(
        implicitReceiverValues: List<ImplicitReceiverValue<*>>,
        info: CallInfo,
        collector: CandidateCollector,
        qualifiedReceiver: FirResolvedQualifier
    ): CandidateCollector {
        val qualifierScope = FirExplicitSimpleImportingScope(
            listOf(
                FirResolvedImportImpl(
                    FirImportImpl(null, FqName.topLevel(info.name), false, null),
                    qualifiedReceiver.packageFqName,
                    qualifiedReceiver.relativeClassFqName
                )
            ), session, components.scopeSession
        )
        val candidateFactory = CandidateFactory(components, info)

        when (info.callKind) {
            CallKind.VariableAccess -> {
                qualifierScope.processPropertiesByName(info.name) {
                    collector.consumeCandidate(0, candidateFactory.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER))
                }
                qualifierScope.processClassifiersByName(info.name) {
                    if (it is FirClassSymbol<*> && it.fir.classKind == ClassKind.OBJECT) {
                        collector.consumeCandidate(0, candidateFactory.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER))
                    }
                }
            }
            CallKind.Function -> {
                qualifierScope.processFunctionsAndConstructorsByName(info.name, session, components) {
                    collector.consumeCandidate(0, candidateFactory.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER))
                }

                val invokeReceiverCollector = CandidateCollector(components, components.resolutionStageRunner)
                val invokeReceiverCandidateFactory = CandidateFactory(
                    components,
                    info.replaceWithVariableAccess()
                )

                qualifierScope.processPropertiesByName(info.name) {
                    invokeReceiverCollector.consumeCandidate(
                        0, invokeReceiverCandidateFactory.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER)
                    )
                }
                if (invokeReceiverCollector.isSuccess()) {
                    for (invokeReceiverCandidate in invokeReceiverCollector.bestCandidates()) {
                        val invokeReceiverExpression = createExplicitReceiverForInvoke(invokeReceiverCandidate).let {
                            components.transformQualifiedAccessUsingSmartcastInfo(it)
                        }
                        runResolver(
                            implicitReceiverValues,
                            info.copy(explicitReceiver = invokeReceiverExpression, name = OperatorNameConventions.INVOKE),
                            collector
                        )
                    }
                }
            }
            CallKind.CallableReference -> return collector
            else -> throw AssertionError("Unsupported call kind in tower resolver: ${info.callKind}")
        }
        return collector
    }

    fun runResolver(
        implicitReceiverValues: List<ImplicitReceiverValue<*>>,
        info: CallInfo,
        collector: CandidateCollector = this.collector
    ): CandidateCollector {
        this.implicitReceiverValues = implicitReceiverValues

        val receiver = info.explicitReceiver
        if (receiver is FirResolvedQualifier && receiver.classId == null) {
            return runResolverForFullyQualifiedReceiver(implicitReceiverValues, info, collector, receiver)
        }

        towerDataConsumer = towerDataConsumer(info, collector)
        val shouldProcessExtensionsBeforeMembers =
            info.callKind == CallKind.Function && info.name in HIDES_MEMBERS_NAME_LIST
        val shouldProcessExplicitReceiverScopeOnly =
            info.callKind == CallKind.Function && info.explicitReceiver?.typeRef?.coneTypeSafe<ConeIntegerLiteralType>() != null

        var group = 0

        // Specific case when extension should be processed before members (Kotlin forEach vs Java forEach)
        if (shouldProcessExtensionsBeforeMembers) {
            for (topLevelScope in topLevelScopes) {
                group = processTopLevelScope(towerDataConsumer, topLevelScope, group, extensionsOnly = true)
            }
        }

        // Member of explicit receiver' type (this stage does nothing without explicit receiver)
        // class Foo(val x: Int)
        // fun test(f: Foo) { f.x }
        towerDataConsumer.consume(EMPTY, TowerScopeLevel.Empty, group++)

        if (shouldProcessExplicitReceiverScopeOnly) {
            return collector
        }

        // Member of local scope
        // fun test(x: Int) = x
        val nonEmptyLocalScopes = mutableListOf<FirLocalScope>()
        for (scope in localScopes) {
            if (towerDataConsumer.consume(TOWER_LEVEL, ScopeTowerLevel(session, components, scope), group++) != NONE) {
                nonEmptyLocalScopes += scope
            }
        }

        var blockDispatchReceivers = false

        // Member of implicit receiver' type *and* relevant scope
        for (implicitReceiverValue in implicitReceiverValues) {
            if (!blockDispatchReceivers || implicitReceiverValue !is ImplicitDispatchReceiverValue) {
                // Direct use of implicit receiver (see inside)
                group = processImplicitReceiver(towerDataConsumer, implicitReceiverValue, nonEmptyLocalScopes, group)
            }
            val implicitScope = implicitReceiverValue.implicitScope
            if (implicitScope != null) {
                // Regular implicit receiver scope (outer objects, statics)
                // object Outer {
                //     val x = 0
                //     class Nested { val y = x }
                // }
                towerDataConsumer.consume(TOWER_LEVEL, ScopeTowerLevel(session, components, implicitScope), group++)
            }
            if (implicitReceiverValue is ImplicitDispatchReceiverValue) {
                val implicitCompanionScopes = implicitReceiverValue.implicitCompanionScopes
                for (implicitCompanionScope in implicitCompanionScopes) {
                    // Companion scope bound to implicit receiver scope
                    // class Outer {
                    //     companion object { val x = 0 }
                    //     class Nested { val y = x }
                    // }
                    towerDataConsumer.consume(TOWER_LEVEL, ScopeTowerLevel(session, components, implicitCompanionScope), group++)
                }
                if ((implicitReceiverValue.boundSymbol.fir as? FirRegularClass)?.isInner == false) {
                    blockDispatchReceivers = true
                }
            }
        }

        for (topLevelScope in topLevelScopes) {
            group = processTopLevelScope(towerDataConsumer, topLevelScope, group)
        }

        return collector
    }
}
