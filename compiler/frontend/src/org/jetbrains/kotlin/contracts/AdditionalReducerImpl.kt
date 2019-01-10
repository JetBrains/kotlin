/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts

import org.jetbrains.kotlin.contracts.model.structure.ESLambdaParameterReceiverReference
import org.jetbrains.kotlin.contracts.model.structure.ESReceiver
import org.jetbrains.kotlin.contracts.model.structure.ESReceiverValue
import org.jetbrains.kotlin.contracts.model.visitors.AdditionalReducer

class AdditionalReducerImpl : AdditionalReducer {
    override fun reduceReceiverReference(esReceiverReference: ESLambdaParameterReceiverReference): ESReceiver? {
        val lambda = esReceiverReference.lambda as? ESLambda ?: throw AssertionError()
        val receiverValue = lambda.receiverValue ?: throw AssertionError()
        return ESReceiverValue(receiverValue)
    }
}