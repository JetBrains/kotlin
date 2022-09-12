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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.sam.SamConversionOracle
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.checker.NewKotlinTypeChecker


class KotlinCallComponents(
    val statelessCallbacks: KotlinResolutionStatelessCallbacks,
    val argumentsToParametersMapper: ArgumentsToParametersMapper,
    val typeArgumentsToParametersMapper: TypeArgumentsToParametersMapper,
    val constraintInjector: ConstraintInjector,
    val reflectionTypes: ReflectionTypes,
    val builtIns: KotlinBuiltIns,
    val languageVersionSettings: LanguageVersionSettings,
    val samConversionOracle: SamConversionOracle,
    val samConversionResolver: SamConversionResolver,
    val kotlinTypeChecker: NewKotlinTypeChecker,
    val lookupTracker: LookupTracker,
    val kotlinTypeRefiner: KotlinTypeRefiner,
    val callableReferenceArgumentResolver: CallableReferenceArgumentResolver
)

class GivenCandidate(
    val descriptor: FunctionDescriptor,
    val dispatchReceiver: ReceiverValueWithSmartCastInfo?,
    val knownTypeParametersResultingSubstitutor: TypeSubstitutor?
)
