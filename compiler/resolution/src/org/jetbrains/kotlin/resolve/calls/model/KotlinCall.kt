/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.model

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


interface KotlinCall : ResolutionAtom {
    val callKind: KotlinCallKind

    val explicitReceiver: ReceiverKotlinCallArgument?

    // a.(foo)() -- (foo) is dispatchReceiverForInvoke
    val dispatchReceiverForInvokeExtension: ReceiverKotlinCallArgument? get() = null

    val name: Name

    val typeArguments: List<TypeArgument>

    val argumentsInParenthesis: List<KotlinCallArgument>

    val externalArgument: KotlinCallArgument?
}

private fun SimpleKotlinCallArgument.checkReceiverInvariants() {
    assert(!isSpread) {
        "Receiver cannot be a spread: $this"
    }
    assert(argumentName == null) {
        "Argument name should be null for receiver: $this, but it is $argumentName"
    }
    checkArgumentInvariants()
}

private fun KotlinCallArgument.checkArgumentInvariants() {
    if (this is SubKotlinCallArgument) {
        assert(callResult.type == CallResolutionResult.Type.PARTIAL) {
            "SubCall should has type PARTIAL: $callResult"
        }
        assert(callResult.resultCallAtom != null) {
            "SubCall should has resultCallAtom: $callResult"
        }
    }
}

fun KotlinCall.checkCallInvariants() {
    assert(explicitReceiver !is LambdaKotlinCallArgument && explicitReceiver !is CallableReferenceKotlinCallArgument) {
        "Lambda argument or callable reference is not allowed as explicit receiver: $explicitReceiver"
    }

    explicitReceiver.safeAs<SimpleKotlinCallArgument>()?.checkReceiverInvariants()
    dispatchReceiverForInvokeExtension.safeAs<SimpleKotlinCallArgument>()?.checkReceiverInvariants()
    argumentsInParenthesis.forEach(KotlinCallArgument::checkArgumentInvariants)
    externalArgument?.checkArgumentInvariants()

    if (callKind != KotlinCallKind.FUNCTION) {
        assert(externalArgument == null) {
            "External argument is not allowed not for function call: $externalArgument."
        }
        assert(argumentsInParenthesis.isEmpty()) {
            "Arguments in parenthesis should be empty for not function call: $this "
        }
        assert(dispatchReceiverForInvokeExtension == null) {
            "Dispatch receiver for invoke should be null for not function call: $dispatchReceiverForInvokeExtension"
        }
    }
    else {
        assert(externalArgument == null || !externalArgument!!.isSpread) {
            "External argument cannot nave spread element: $externalArgument"
        }

        assert(externalArgument?.argumentName == null) {
            "Illegal external argument with name: $externalArgument"
        }

        assert(dispatchReceiverForInvokeExtension == null || !dispatchReceiverForInvokeExtension!!.isSafeCall) {
            "Dispatch receiver for invoke cannot be safe: $dispatchReceiverForInvokeExtension"
        }
    }
}
