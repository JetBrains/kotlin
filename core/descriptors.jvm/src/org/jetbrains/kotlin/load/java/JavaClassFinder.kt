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

package org.jetbrains.kotlin.load.java

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

interface JavaClassFinder {
    data class Request(
        val classId: ClassId,
        @Suppress("ArrayInDataClass")
        val previouslyFoundClassFileContent: ByteArray? = null,
        val outerClass: JavaClass? = null
    )

    fun findClass(request: Request): JavaClass?
    fun findClass(classId: ClassId): JavaClass? = findClass(Request(classId))

    fun findPackage(fqName: FqName): JavaPackage?

    fun knownClassNamesInPackage(packageFqName: FqName): Set<String>?
}
