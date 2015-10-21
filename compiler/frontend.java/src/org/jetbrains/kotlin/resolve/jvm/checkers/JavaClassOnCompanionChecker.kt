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

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.KotlinTypeImpl
import org.jetbrains.kotlin.types.TypeProjectionImpl

public class JavaClassOnCompanionChecker : CallChecker {
    override fun <F : CallableDescriptor> check(resolvedCall: ResolvedCall<F>, context: BasicCallResolutionContext) {
        val descriptor = resolvedCall.resultingDescriptor
        if (descriptor !is PropertyDescriptor || descriptor.name.asString() != "javaClass") return

        val container = descriptor.containingDeclaration
        if (container !is PackageFragmentDescriptor || container.fqName.asString() != "kotlin") return

        val actualType = descriptor.type

        val companionObject = actualType.arguments.singleOrNull()?.type?.constructor?.declarationDescriptor as? ClassDescriptor ?: return
        if (companionObject.isCompanionObject) {
            val containingClass = companionObject.containingDeclaration as ClassDescriptor
            val javaLangClass = actualType.constructor.declarationDescriptor as? ClassDescriptor ?: return
            val expectedType = KotlinTypeImpl.create(
                    Annotations.EMPTY, javaLangClass, actualType.isMarkedNullable,
                    listOf(TypeProjectionImpl(containingClass.defaultType))
            )
            context.trace.report(ErrorsJvm.JAVA_CLASS_ON_COMPANION.on(resolvedCall.call.callElement, actualType, expectedType))
        }
    }
}
