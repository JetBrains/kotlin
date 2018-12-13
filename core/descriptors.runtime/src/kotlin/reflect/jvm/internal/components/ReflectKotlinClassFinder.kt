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

package kotlin.reflect.jvm.internal.components

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder.Result.KotlinClass
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.InputStream

class ReflectKotlinClassFinder(private val classLoader: ClassLoader) : KotlinClassFinder {
    private fun findKotlinClass(fqName: String): KotlinClassFinder.Result? {
        return classLoader.tryLoadClass(fqName)?.let { ReflectKotlinClass.create(it) }?.let(::KotlinClass)
    }

    override fun findKotlinClassOrContent(classId: ClassId) = findKotlinClass(classId.toRuntimeFqName())

    override fun findKotlinClassOrContent(javaClass: JavaClass): KotlinClassFinder.Result? {
        // TODO: go through javaClass's class loader
        return findKotlinClass(javaClass.fqName?.asString() ?: return null)
    }

    // TODO
    override fun findMetadata(classId: ClassId): InputStream? = null

    // TODO
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    // TODO: load built-ins from classLoader
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null
}

private fun ClassId.toRuntimeFqName(): String {
    val className = relativeClassName.asString().replace('.', '$')
    return if (packageFqName.isRoot) className else "$packageFqName.$className"
}
