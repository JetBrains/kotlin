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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.decompiler.isKotlinWithCompatibleAbiVersion
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.LocalClassResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil

class DirectoryBasedClassFinder(
        val packageDirectory: VirtualFile,
        val directoryPackageFqName: FqName
) : KotlinClassFinder {
    override fun findKotlinClass(javaClass: JavaClass) = findKotlinClass(javaClass.classId)

    override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
        if (classId.getPackageFqName() != directoryPackageFqName) {
            return null
        }
        val targetName = classId.getRelativeClassName().pathSegments().joinToString("$", postfix = ".class")
        val virtualFile = packageDirectory.findChild(targetName)
        if (virtualFile != null && isKotlinWithCompatibleAbiVersion(virtualFile)) {
            return KotlinBinaryClassCache.getKotlinBinaryClass(virtualFile)
        }
        return null
    }
}

class DirectoryBasedDataFinder(
        val classFinder: DirectoryBasedClassFinder,
        val log: Logger
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val binaryClass = classFinder.findKotlinClass(classId) ?: return null
        val data = binaryClass.getClassHeader().annotationData
        if (data == null) {
            log.error("Annotation data missing for ${binaryClass.getClassId()}")
            return null
        }
        return JvmProtoBufUtil.readClassDataFrom(data)
    }
}

object ResolveEverythingToKotlinAnyLocalClassResolver : LocalClassResolver {
    override fun resolveLocalClass(classId: ClassId): ClassDescriptor = KotlinBuiltIns.getInstance().getAny()
}

private val JavaClass.classId: ClassId
    get() {
        val outer = getOuterClass()
        return if (outer == null) ClassId.topLevel(getFqName()!!) else outer.classId.createNestedClassId(getName())
    }
