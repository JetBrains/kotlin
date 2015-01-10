/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.decompiler.textBuilder

import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.DeserializedResolverUtils
import org.jetbrains.jet.plugin.decompiler.isKotlinWithCompatibleAbiVersion
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import com.intellij.openapi.diagnostic.Logger

class LocalClassFinder(
        val packageDirectory: VirtualFile,
        val directoryPackageFqName: FqName
) : KotlinClassFinder {
    override fun findKotlinClass(javaClass: JavaClass) = findKotlinClass(javaClass.classId)

    override fun findKotlinClass(classId: ClassId): KotlinJvmBinaryClass? {
        if (classId.getPackageFqName() != directoryPackageFqName) {
            return null
        }
        val segments = DeserializedResolverUtils.kotlinFqNameToJavaFqName(classId.getRelativeClassName()).pathSegments()
        val targetName = segments.joinToString("$", postfix = ".class")
        val virtualFile = packageDirectory.findChild(targetName)
        if (virtualFile != null && isKotlinWithCompatibleAbiVersion(virtualFile)) {
            return KotlinBinaryClassCache.getKotlinBinaryClass(virtualFile)
        }
        return null
    }
}

class LocalClassDataFinder(
        val localClassFinder: LocalClassFinder,
        val log: Logger
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val binaryClass = localClassFinder.findKotlinClass(classId) ?: return null
        val data = binaryClass.getClassHeader().annotationData
        if (data == null) {
            log.error("Annotation data missing for ${binaryClass.getClassId()}")
            return null
        }
        return JvmProtoBufUtil.readClassDataFrom(data)
    }
}

private val JavaClass.classId: ClassId
    get() {
        val outer = getOuterClass()
        return if (outer == null) ClassId.topLevel(getFqName()!!) else outer.classId.createNestedClassId(getName())
    }
