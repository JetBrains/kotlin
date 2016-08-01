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
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.tower.CandidateWithBoundDispatchReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.UnwrappedType


interface ReceiverKotlinCallArgument {
    val receiver: DetailedReceiver
}

class QualifierReceiverKotlinCallArgument(override val receiver: QualifierReceiver) : ReceiverKotlinCallArgument {
    override fun toString() = "$receiver"
}

interface KotlinCallArgument {
    val isSpread: Boolean
    val argumentName: Name?
}

interface SimpleKotlinCallArgument : KotlinCallArgument, ReceiverKotlinCallArgument {
    override val receiver: ReceiverValueWithSmartCastInfo

    val isSafeCall: Boolean
}

interface ExpressionKotlinCallArgument : SimpleKotlinCallArgument

interface SubKotlinCallArgument : SimpleKotlinCallArgument {
    val resolvedCall: ResolvedKotlinCall.OnlyResolvedKotlinCall
}

interface LambdaKotlinCallArgument : KotlinCallArgument {
    override val isSpread: Boolean
        get() = false

    /**
     * parametersTypes == null means, that there is no declared arguments
     * null inside array means that this type is not declared explicitly
     */
    val parametersTypes: Array<UnwrappedType?>?
}

interface FunctionExpression : LambdaKotlinCallArgument {
    override val parametersTypes: Array<UnwrappedType?>

    // null means that there function can not have receiver
    val receiverType: UnwrappedType?

    // null means that return type is not declared, for fun(){ ... } returnType == Unit
    val returnType: UnwrappedType?
}

interface CallableReferenceKotlinCallArgument : KotlinCallArgument {
    override val isSpread: Boolean
        get() = false

    // Foo::bar lhsType = Foo. For a::bar where a is expression, this type is null
    val lhsType: UnwrappedType?

    val constraintStorage: ConstraintStorage
}

interface ChosenCallableReferenceDescriptor : CallableReferenceKotlinCallArgument {
    val candidate: CandidateWithBoundDispatchReceiver

    val extensionReceiver: ReceiverValueWithSmartCastInfo?
}


interface TypeArgument

// todo allow '_' in frontend
object TypeArgumentPlaceholder : TypeArgument

interface SimpleTypeArgument: TypeArgument {
    val type: UnwrappedType
}
