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

import com.intellij.debugger.engine.evaluation.EvaluateException
import com.sun.jdi.ClassLoaderReference
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext
import org.jetbrains.kotlin.idea.debugger.evaluate.LOG
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassLoadingAdapter
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.ClassToLoad

sealed class ClassLoadingResult {
    class Success(val classLoader: ClassLoaderReference) : ClassLoadingResult()
    class Failure(val error: Throwable) : ClassLoadingResult()
    object NotNeeded : ClassLoadingResult()
}

fun loadClassesSafely(context: ExecutionContext, classes: Collection<ClassToLoad>): ClassLoadingResult {
    if (classes.isEmpty()) {
        return ClassLoadingResult.NotNeeded
    }

    return try {
        val cl = loadClasses(context, classes)
        if (cl != null) {
            ClassLoadingResult.Success(cl)
        } else {
            ClassLoadingResult.NotNeeded
        }
    } catch (e: EvaluateException) {
        throw e
    } catch (e: Throwable) {
        LOG.debug("Failed to evaluate expression", e)
        ClassLoadingResult.Failure(e)
    }
}

fun loadClasses(context: ExecutionContext, classes: Collection<ClassToLoad>): ClassLoaderReference? {
    return ClassLoadingAdapter.loadClasses(context, classes)
}