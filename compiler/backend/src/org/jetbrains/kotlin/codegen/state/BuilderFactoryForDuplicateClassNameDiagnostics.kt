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
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import java.util.concurrent.ConcurrentHashMap

class BuilderFactoryForDuplicateClassNameDiagnostics(
    builderFactory: ClassBuilderFactory,
    private val state: GenerationState,
) : ClassNameCollectionClassBuilderFactory(builderFactory) {

    private val className = ConcurrentHashMap<String, JvmDeclarationOrigin>()

    override fun handleClashingNames(internalName: String, origin: JvmDeclarationOrigin) {
        val another = className.getOrPut(internalName) { origin }
        // Allow clashing classes if they are originated from the same source element. For example, this happens during inlining anonymous
        // objects. In JVM IR, this also happens for anonymous classes in default arguments of tailrec functions, because default arguments
        // are deep-copied (see JvmTailrecLowering).
        if (origin.originalSourceElement != another.originalSourceElement) {
            reportError(internalName, origin, another)
        }
    }

    private fun reportError(internalName: String, vararg another: JvmDeclarationOrigin) {
        val duplicateClasses =
            another.mapNotNull { it.descriptor }.joinToString { DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(it) }

        for (origin in another) {
            state.reportDuplicateClassNameError(origin, internalName, duplicateClasses)
        }
    }
}
