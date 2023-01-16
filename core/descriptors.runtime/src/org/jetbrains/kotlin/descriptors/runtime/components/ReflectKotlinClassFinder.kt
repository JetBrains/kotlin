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

package org.jetbrains.kotlin.descriptors.runtime.components

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsResourceLoader
import java.io.InputStream

class ReflectKotlinClassFinder(private val classLoader: ClassLoader) : KotlinClassFinder {
    private val builtInsResourceLoader = BuiltInsResourceLoader()

    private fun findKotlinClass(fqName: String): KotlinClassFinder.Result? {
        return classLoader.tryLoadClass(fqName)?.let { ReflectKotlinClass.create(it) }?.let(::KotlinClass)
    }

    override fun findKotlinClassOrContent(classId: ClassId, jvmMetadataVersion: JvmMetadataVersion) =
        findKotlinClass(classId.toRuntimeFqName())

    override fun findKotlinClassOrContent(javaClass: JavaClass, jvmMetadataVersion: JvmMetadataVersion): KotlinClassFinder.Result? {
        // TODO: go through javaClass's class loader
        return findKotlinClass(javaClass.fqName?.asString() ?: return null)
    }

    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? = null

    // TODO
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        if (!packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) return null

        return builtInsResourceLoader.loadResource(BuiltInSerializerProtocol.getBuiltInsFilePath(packageFqName))
    }
}

private fun ClassId.toRuntimeFqName(): String {
    val className = relativeClassName.asString().replace('.', '$')
    return if (packageFqName.isRoot) className else "$packageFqName.$className"
}
