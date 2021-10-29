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

package org.jetbrains.kotlin.resolve.jvm.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.checkers.AbstractReflectionApiCallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.NO_REFLECTION_IN_CLASS_PATH
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue

/**
 * If there's no Kotlin reflection implementation found in the classpath, checks that there are no usages
 * of reflection API which will fail at runtime.
 */
class JvmReflectionAPICallChecker(
    private val module: ModuleDescriptor,
    reflectionTypes: ReflectionTypes,
    storageManager: StorageManager
) : AbstractReflectionApiCallChecker(reflectionTypes, storageManager) {

    override val isWholeReflectionApiAvailable by storageManager.createLazyValue {
        module.findClassAcrossModuleDependencies(JvmAbi.REFLECTION_FACTORY_IMPL) != null
    }

    override fun report(element: PsiElement, context: CallCheckerContext) {
        context.trace.report(NO_REFLECTION_IN_CLASS_PATH.on(element))
    }
}
