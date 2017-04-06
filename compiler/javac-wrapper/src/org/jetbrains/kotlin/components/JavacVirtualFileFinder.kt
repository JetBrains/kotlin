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

package org.jetbrains.kotlin.components

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.wrappers.trees.computeClassId


abstract class JavacVirtualFileFinder : VirtualFileFinder() {

    override fun findKotlinClass(javaClass: JavaClass): KotlinJvmBinaryClass? {
        var file = javaClass.computeClassId()?.let(this::findVirtualFileWithHeader) ?: return null

        if (javaClass.outerClass != null) {
            // For nested classes we get a file of the containing class, to get the actual class file for A.B.C,
            // we take the file for A, take its parent directory, then in this directory we look for A$B$C.class
            file = file.parent!!.findChild(classFileName(javaClass) + ".class").sure { "Virtual file not found for $javaClass" }
        }

        return KotlinBinaryClassCache.getKotlinBinaryClass(file)
    }

    inline fun <T : Any> T?.sure(message: () -> String): T = this ?: throw AssertionError(message())
}