/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.evaluate.compilingEvaluator

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import org.jetbrains.kotlin.idea.debugger.evaluate.LOG
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassLoaderHandler
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassLoadingAdapter
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassToLoad

fun loadClassesSafely(evaluationContext: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderHandler? {
    try {
        return loadClasses(evaluationContext, classes)
    } catch (e: Throwable) {
        LOG.debug("Failed to evaluate expression", e)
        return null
    }
}

fun loadClasses(evaluationContext: EvaluationContextImpl, classes: Collection<ClassToLoad>): ClassLoaderHandler? {
    if (classes.isEmpty()) return ClassLoaderHandler(evaluationContext.classLoader)

    return ClassLoadingAdapter.loadClasses(evaluationContext, classes)?.apply {
        evaluationContext.classLoader = this.reference
    }
}