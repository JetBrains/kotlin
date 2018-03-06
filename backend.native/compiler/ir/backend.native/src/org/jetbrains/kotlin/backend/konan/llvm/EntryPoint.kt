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

package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.descriptors.isArray
import org.jetbrains.kotlin.backend.konan.report
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.alwaysTrue

internal fun findMainEntryPoint(context: Context): FunctionDescriptor? {

    val config = context.config.configuration
    if (config.get(KonanConfigKeys.PRODUCE) != PROGRAM) return null

    val entryPoint = FqName(config.get(KonanConfigKeys.ENTRY) ?:
            if (context.shouldGenerateTestRunner()) testEntryName else defaultEntryName)

    val entryName = entryPoint.shortName()
    val packageName = entryPoint.parent()

    val packageScope = context.builtIns.builtInsModule.getPackage(packageName).memberScope

    val main = packageScope.getContributedFunctions(entryName,
        NoLookupLocation.FROM_BACKEND).singleOrNull {
            it.returnType?.isUnit() == true &&
            it.hasSingleArrayOfStringParameter &&
            it.typeParameters.isEmpty() &&
            it.visibility.isPublicAPI
        }
    if (main == null) {
        context.reportCompilationError("Could not find '$entryName' in '$packageName' package.")
    }
    return main
}

private val defaultEntryName = "main"
private val testEntryName = "konan.test.main"

private val defaultEntryPackage = FqName.ROOT

private val KotlinType.filterClass: ClassDescriptor?
    get() {
        val constr = constructor.declarationDescriptor
        return constr as? ClassDescriptor
    }

private val ClassDescriptor.isString
    get() = fqNameSafe.asString() == "kotlin.String"

private val KotlinType.isString
    get() = filterClass?.isString ?: false

private val KotlinType.isArrayOfString: Boolean
    get() = (filterClass?.isArray ?: false) && 
            (arguments.singleOrNull()?.type?.isString ?: false)

private val FunctionDescriptor.hasSingleArrayOfStringParameter: Boolean
    get() = valueParameters.singleOrNull()?.type?.isArrayOfString ?: false

