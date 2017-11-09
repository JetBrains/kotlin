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

import java.util.HashMap
import java.util.HashSet

class InlineResult private constructor() {

    private val notChangedTypes = hashSetOf<String>()
    private val classesToRemove = HashSet<String>()
    private val changedTypes = HashMap<String, String>()
    val reifiedTypeParametersUsages = ReifiedTypeParametersUsages()

    fun merge(child: InlineResult) {
        classesToRemove.addAll(child.calcClassesToRemove())
    }

    fun mergeWithNotChangeInfo(child: InlineResult) {
        notChangedTypes.addAll(child.notChangedTypes)
        merge(child)
    }

    fun addClassToRemove(classInternalName: String) {
        classesToRemove.add(classInternalName)
    }

    fun addNotChangedClass(classInternalName: String) {
        notChangedTypes.add(classInternalName)
    }

    fun addChangedType(oldClassInternalName: String, newClassInternalName: String) {
        changedTypes.put(oldClassInternalName, newClassInternalName)
    }


    fun calcClassesToRemove(): Set<String> {
        return classesToRemove - notChangedTypes
    }

    fun getChangedTypes(): Map<String, String> {
        return changedTypes
    }

    companion object {
        @JvmStatic
        fun create(): InlineResult {
            return InlineResult()
        }
    }
}
