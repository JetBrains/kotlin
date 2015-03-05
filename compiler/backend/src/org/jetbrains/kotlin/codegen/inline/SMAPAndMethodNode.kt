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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.kotlin.codegen.optimization.common.InsnStream
import org.jetbrains.org.objectweb.asm.tree.LineNumberNode
import org.jetbrains.org.objectweb.asm.Label
import kotlin.properties.Delegates
import java.util.Collections
import kotlin.properties.ReadOnlyProperty
import org.jetbrains.kotlin.codegen.SourceInfo

//TODO comment
class SMAPAndMethodNode(val node: MethodNode, val classSMAP: SMAP) {

    val lineNumbers =
        InsnStream(node.instructions.getFirst(), null).stream().filterIsInstance<LineNumberNode>().map {
            val index = Collections.binarySearch(classSMAP.intervals, RangeMapping(it.line, it.line, 1)) {
                (value, key) ->
                if (value.contains(key.dest)) 0 else RangeMapping.Comparator.compare(value, key)
            }
            if (index < 0)
                throw IllegalStateException("Unmapped label in inlined function $it ${it.line}")
            LabelAndMapping(it, classSMAP.intervals[index])
        }.toList()

    val ranges = lineNumbers.stream().map { it.mapper }.toList().distinct().toList();
}

class LabelAndMapping(val lineNumberNode: LineNumberNode, val mapper: RangeMapping)