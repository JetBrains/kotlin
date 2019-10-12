/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import androidx.compose.plugins.kotlin.analysis.ComposeDefaultErrorMessages
import androidx.compose.plugins.kotlin.analysis.ComposeErrors
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.checkers.CallCheckerContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

open class TryCatchComposableChecker : CallChecker, StorageComponentContainerContributor {

    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        if (!platform.isJvm()) return
        container.useInstance(this)
    }

    override fun check(
        resolvedCall: ResolvedCall<*>,
        reportOn: PsiElement,
        context: CallCheckerContext
    ) {
        val trace = context.trace
        val call = resolvedCall.call.callElement
        val shouldBeTag =
            ComposableAnnotationChecker.get(call.project).shouldInvokeAsTag(trace, resolvedCall)
        if (shouldBeTag) {
            var walker: PsiElement? = call
            while (walker != null) {
                val parent = walker.parent
                if (parent is KtTryExpression) {
                    if (walker == parent.tryBlock)
                        trace.reportFromPlugin(
                            ComposeErrors.ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE.on(
                                parent.tryKeyword!!
                            ),
                            ComposeDefaultErrorMessages
                        )
                }
                walker = try { walker.parent } catch (e: Throwable) { null }
            }
        }
    }
}
