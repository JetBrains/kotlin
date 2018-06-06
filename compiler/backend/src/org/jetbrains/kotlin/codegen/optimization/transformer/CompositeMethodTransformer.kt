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

package org.jetbrains.kotlin.codegen.optimization.transformer

import org.jetbrains.org.objectweb.asm.tree.MethodNode

open class CompositeMethodTransformer(private val transformers: List<MethodTransformer>) : MethodTransformer() {
    constructor(vararg transformers: MethodTransformer?) : this(transformers.filterNotNull())

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        transformers.forEach { it.transform(internalClassName, methodNode) }
    }

    companion object {
        inline fun build(builder: MutableList<MethodTransformer>.() -> Unit) =
            CompositeMethodTransformer(ArrayList<MethodTransformer>().apply { builder() })
    }
}