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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.UnwrappedType


interface ReceiverKotlinCallArgument : KotlinCallArgument {
    val receiver: DetailedReceiver
    val isSafeCall: Boolean
}

class QualifierReceiverKotlinCallArgument(override val receiver: QualifierReceiver) : ReceiverKotlinCallArgument {
    override val isSafeCall: Boolean
        get() = false // TODO: add warning

    override fun toString() = "$receiver"

    override val isSpread get() = false
    override val argumentName: Name? get() = null
}

interface KotlinCallArgument {
    val isSpread: Boolean
    val argumentName: Name?
}

interface PostponableKotlinCallArgument : KotlinCallArgument, ResolutionAtom

interface SimpleKotlinCallArgument : KotlinCallArgument, ReceiverKotlinCallArgument {
    override val receiver: ReceiverValueWithSmartCastInfo
}

interface ExpressionKotlinCallArgument : SimpleKotlinCallArgument, ResolutionAtom

interface SubKotlinCallArgument : SimpleKotlinCallArgument {
    val callResult: CallResolutionResult
}

interface LambdaKotlinCallArgument : PostponableKotlinCallArgument {
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

/**
 * cases: class A {}, class B { companion object }, object C, enum class D { E }
 * A::foo <-> Type
 * a::foo <-> Expression
 * B::foo <-> Type
 * C::foo <-> Object
 * D.E::foo <-> Expression
 */
sealed class LHSResult {
    class Type(val qualifier: QualifierReceiver, resolvedType: UnwrappedType): LHSResult() {
        val unboundDetailedReceiver: ReceiverValueWithSmartCastInfo

        init {
            assert(qualifier.descriptor is ClassDescriptor) {
                "Should be ClassDescriptor: ${qualifier.descriptor}"
            }

            val unboundReceiver = TransientReceiver(resolvedType)
            unboundDetailedReceiver = ReceiverValueWithSmartCastInfo(unboundReceiver, emptySet(), isStable = true)
        }
    }

    class Object(val qualifier: QualifierReceiver): LHSResult() {
        val objectValueReceiver: ReceiverValueWithSmartCastInfo

        init {
            assert(DescriptorUtils.isObject(qualifier.descriptor)) {
                "Should be object descriptor: ${qualifier.descriptor}"
            }
            objectValueReceiver = qualifier.classValueReceiverWithSmartCastInfo ?: error("class value should be not null for $qualifier")
        }
    }
    class Expression(val lshCallArgument: SimpleKotlinCallArgument): LHSResult()

    // todo this case is forbid for now
    object Empty: LHSResult()

    object Error: LHSResult()
}

interface CallableReferenceKotlinCallArgument : PostponableKotlinCallArgument {
    override val isSpread: Boolean
        get() = false

    val lhsResult: LHSResult

    val rhsName: Name
}

interface CollectionLiteralKotlinCallArgument : PostponableKotlinCallArgument

interface TypeArgument

// todo allow '_' in frontend
object TypeArgumentPlaceholder : TypeArgument

interface SimpleTypeArgument: TypeArgument {
    val type: UnwrappedType
}
