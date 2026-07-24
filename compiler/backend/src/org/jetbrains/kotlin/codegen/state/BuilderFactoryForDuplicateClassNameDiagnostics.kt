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

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassNameCollectionClassBuilderFactory
import org.jetbrains.kotlin.ir.declarations.IrClass
import java.util.concurrent.ConcurrentHashMap

class BuilderFactoryForDuplicateClassNameDiagnostics(
    builderFactory: ClassBuilderFactory,
    private val state: GenerationState,
) : ClassNameCollectionClassBuilderFactory(builderFactory) {
    private val className = ConcurrentHashMap<String, Any>()

    override fun handleClashingNames(internalName: String, origin: IrClass?) {
        val another = className.getOrPut(internalName) { origin ?: NO_ORIGIN }.takeUnless { it === NO_ORIGIN } as IrClass?
        // Allow clashing classes if they are originated from the same source element. For example, this happens during inlining anonymous
        // objects. In JVM IR, this also happens for anonymous classes in default arguments of tailrec functions, because default arguments
        // are deep-copied (see JvmTailrecLowering).
        if (origin != null && another != null && origin.attributeOwnerId != another.attributeOwnerId) {
            state.reportDuplicateClassNameError(origin, internalName, another)
            state.reportDuplicateClassNameError(another, internalName, origin)
        }
    }

    private companion object {
        val NO_ORIGIN = Any()
    }
}
