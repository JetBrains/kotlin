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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.state.GenerationState

typealias ClassBuilderFactoryProvider = GenerationState.Builder.() -> ClassBuilderFactory

fun classBuilderFactoryForBinaries(generateSourceRetentionAnnotations: Boolean): ClassBuilderFactoryProvider =
        classBuilderFactoryForBinaries(ClassBuilderMode.full(generateSourceRetentionAnnotations))

fun classBuilderFactoryForBinaries(classBuilderMode: ClassBuilderMode): ClassBuilderFactoryProvider =
        { BinaryClassBuilderFactory(classBuilderMode, jvmBackendClassResolver) }

fun classBuilderFactoryForTests() =
        classBuilderFactoryWithTrace(false)

fun classBuilderFactoryWithTrace(generateSourceRetentionAnnotations: Boolean): ClassBuilderFactoryProvider =
        { TestClassBuilderFactory(generateSourceRetentionAnnotations, jvmBackendClassResolver) }

fun classBuilderFactoryWithTrace(classBuilderMode: ClassBuilderMode): ClassBuilderFactoryProvider =
        { TestClassBuilderFactory(classBuilderMode, jvmBackendClassResolver) }

fun givenClassBuilderFactory(classBuilderFactory: ClassBuilderFactory): ClassBuilderFactoryProvider =
        { classBuilderFactory }