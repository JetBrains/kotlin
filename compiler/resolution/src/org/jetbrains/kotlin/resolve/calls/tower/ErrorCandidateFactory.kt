/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tower.WrongResolutionToClassifier.*
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import org.jetbrains.kotlin.utils.SmartList

enum class WrongResolutionToClassifier(val message: (Name) -> String) {
    TYPE_PARAMETER_AS_VALUE({ "Type parameter $it cannot be used as value" }),
    TYPE_PARAMETER_AS_FUNCTION({ "Type parameter $it cannot be called as function" }),
    INTERFACE_AS_VALUE({ "Interface $it does not have companion object" }),
    INTERFACE_AS_FUNCTION({ "Interface $it does not have constructors" }),
    CLASS_AS_VALUE({ "Class $it does not have companion object" }),
    INNER_CLASS_CONSTRUCTOR_NO_RECEIVER({ "Constructor of inner class $it can be called only with receiver of containing class" }),
    OBJECT_AS_FUNCTION({ "Function 'invoke()' is not found in object $it" })
}

sealed class ErrorCandidate<out D: DeclarationDescriptor>(val descriptor: D) {
    class Classifier(
            classifierDescriptor: ClassifierDescriptor,
            val kind: WrongResolutionToClassifier
    ) : ErrorCandidate<ClassifierDescriptor>(classifierDescriptor) {
        val errorMessage = kind.message(descriptor.name)
    }
}

fun collectErrorCandidatesForFunction(
        scopeTower: ImplicitScopeTower,
        name: Name,
        explicitReceiver: DetailedReceiver?
): Collection<ErrorCandidate<*>> {
    val context = ErrorCandidateContext(scopeTower, name, explicitReceiver)
    context.asClassifierCall(asFunction = true)
    return context.result
}

fun collectErrorCandidatesForVariable(
        scopeTower: ImplicitScopeTower,
        name: Name,
        explicitReceiver: DetailedReceiver?
): Collection<ErrorCandidate<*>> {
    val context = ErrorCandidateContext(scopeTower, name, explicitReceiver)
    context.asClassifierCall(asFunction = false)
    return context.result
}

private class ErrorCandidateContext(
        val scopeTower: ImplicitScopeTower,
        val name: Name,
        val explicitReceiver: DetailedReceiver?
) {
    val result = SmartList<ErrorCandidate<*>>()

    fun add(errorCandidate: ErrorCandidate<*>) { result.add(errorCandidate) }
}

private fun ErrorCandidateContext.asClassifierCall(asFunction: Boolean) {
    val classifier =
            when (explicitReceiver) {
                is ReceiverValueWithSmartCastInfo -> {
                    explicitReceiver.receiverValue.type.memberScope.getContributedClassifier(name, scopeTower.location)
                }
                is QualifierReceiver -> {
                    explicitReceiver.staticScope.getContributedClassifier(name, scopeTower.location)
                }
                else -> scopeTower.lexicalScope.findClassifier(name, scopeTower.location)
            } ?: return

    val kind =
            when (classifier) {
                is TypeParameterDescriptor -> if (asFunction) TYPE_PARAMETER_AS_FUNCTION else TYPE_PARAMETER_AS_VALUE
                is ClassDescriptor -> {
                    when (classifier.kind) {
                        ClassKind.INTERFACE -> if (asFunction) INTERFACE_AS_FUNCTION else INTERFACE_AS_VALUE
                        ClassKind.OBJECT -> if (asFunction) OBJECT_AS_FUNCTION else return
                        ClassKind.CLASS -> when {
                            asFunction && explicitReceiver is QualifierReceiver? && classifier.isInner -> INNER_CLASS_CONSTRUCTOR_NO_RECEIVER
                            !asFunction -> CLASS_AS_VALUE
                            else -> return
                        }
                        else -> return
                    }
                }
                else -> return
            }


    add(ErrorCandidate.Classifier(classifier, kind))
}
