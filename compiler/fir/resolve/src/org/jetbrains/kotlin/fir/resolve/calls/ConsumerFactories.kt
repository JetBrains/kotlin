/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.name.Name

fun createVariableAndObjectConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    return PrioritizedTowerDataConsumer(
        resultCollector,
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Properties,
            callInfo,
            bodyResolveComponents,
            resultCollector
        ),
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Objects,
            callInfo,
            bodyResolveComponents,
            resultCollector
        )
    )
}

fun createSimpleFunctionConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    return createSimpleConsumer(
        session,
        name,
        TowerScopeLevel.Token.Functions,
        callInfo,
        bodyResolveComponents,
        resultCollector
    )
}

fun createFunctionConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    resultCollector: CandidateCollector,
    towerResolver: FirTowerResolver
): TowerDataConsumer {
    val varCallInfo = callInfo.replaceWithVariableAccess()
    return PrioritizedTowerDataConsumer(
        resultCollector,
        createSimpleConsumer(
            session,
            name,
            TowerScopeLevel.Token.Functions,
            callInfo,
            bodyResolveComponents,
            resultCollector
        ),
        AccumulatingTowerDataConsumer(resultCollector).apply {
            initialConsumer = createSimpleConsumer(
                session,
                name,
                TowerScopeLevel.Token.Properties,
                varCallInfo,
                bodyResolveComponents,
                InvokeReceiverCandidateCollector(
                    towerResolver,
                    invokeCallInfo = callInfo,
                    components = bodyResolveComponents,
                    invokeConsumer = this,
                    resolutionStageRunner = resultCollector.resolutionStageRunner
                )
            )
        }
    )
}

fun createSimpleConsumer(
    session: FirSession,
    name: Name,
    token: TowerScopeLevel.Token<*>,
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    val factory = CandidateFactory(bodyResolveComponents, callInfo)
    val explicitReceiver = callInfo.explicitReceiver
    return if (explicitReceiver != null) {
        ExplicitReceiverTowerDataConsumer(session, name, token, qualifierOrExpressionReceiver(explicitReceiver), factory, resultCollector)
    } else {
        NoExplicitReceiverTowerDataConsumer(session, name, token, factory, resultCollector)
    }
}

fun createCallableReferencesConsumer(
    session: FirSession,
    name: Name,
    callInfo: CallInfo,
    bodyResolveComponents: BodyResolveComponents,
    resultCollector: CandidateCollector
): TowerDataConsumer {
    // TODO: Use SamePriorityConsumer
    return PrioritizedTowerDataConsumer(
        resultCollector,
        createSimpleConsumer(session, name, TowerScopeLevel.Token.Functions, callInfo, bodyResolveComponents, resultCollector),
        createSimpleConsumer(session, name, TowerScopeLevel.Token.Properties, callInfo, bodyResolveComponents, resultCollector)
    )
}
