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

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.incremental.storage.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.supertypes
import java.io.File

/**
 * Incremental cache common for JVM and JS
 */
abstract class IncrementalCacheCommon(workingDir: File) : BasicMapsOwner(workingDir) {
    companion object {
        private val SUBTYPES = "subtypes"
        private val SUPERTYPES = "supertypes"
        private val CLASS_FQ_NAME_TO_SOURCE = "class-fq-name-to-source"
    }

    private val dependents = arrayListOf<IncrementalCacheCommon>()
    fun addDependentCache(cache: IncrementalCacheCommon) {
        dependents.add(cache)
    }
    val thisWithDependentCaches: Iterable<IncrementalCacheCommon> by lazy {
        val result = arrayListOf(this)
        result.addAll(dependents)
        result
    }

    private val subtypesMap = registerMap(SubtypesMap(SUBTYPES.storageFile))
    private val supertypesMap = registerMap(SupertypesMap(SUPERTYPES.storageFile))
    protected val classFqNameToSourceMap = registerMap(ClassFqNameToSourceMap(CLASS_FQ_NAME_TO_SOURCE.storageFile))

    fun getSubtypesOf(className: FqName): Sequence<FqName> =
            subtypesMap[className].asSequence()

    fun getSourceFileIfClass(fqName: FqName): File? =
            classFqNameToSourceMap[fqName]

    abstract fun markDirty(removedAndCompiledSources: List<File>)

    protected fun addToClassStorage(proto: ProtoBuf.Class, nameResolver: NameResolver, srcFile: File) {
        val supertypes = proto.supertypes(TypeTable(proto.typeTable))
        val parents = supertypes.map { nameResolver.getClassId(it.className).asSingleFqName() }
                .filter { it.asString() != "kotlin.Any" }
                .toSet()
        val child = nameResolver.getClassId(proto.fqName).asSingleFqName()

        parents.forEach { subtypesMap.add(it, child) }

        val removedSupertypes = supertypesMap[child].filter { it !in parents }
        removedSupertypes.forEach { subtypesMap.removeValues(it, setOf(child)) }

        supertypesMap[child] = parents
        classFqNameToSourceMap[child] = srcFile
    }

    protected fun removeAllFromClassStorage(removedClasses: Collection<FqName>) {
        if (removedClasses.isEmpty()) return

        val removedFqNames = removedClasses.toSet()

        for (cache in thisWithDependentCaches) {
            val parentsFqNames = hashSetOf<FqName>()
            val childrenFqNames = hashSetOf<FqName>()

            for (removedFqName in removedFqNames) {
                parentsFqNames.addAll(cache.supertypesMap[removedFqName])
                childrenFqNames.addAll(cache.subtypesMap[removedFqName])

                cache.supertypesMap.remove(removedFqName)
                cache.subtypesMap.remove(removedFqName)
            }

            for (child in childrenFqNames) {
                cache.supertypesMap.removeValues(child, removedFqNames)
            }

            for (parent in parentsFqNames) {
                cache.subtypesMap.removeValues(parent, removedFqNames)
            }
        }

        removedFqNames.forEach { classFqNameToSourceMap.remove(it) }
    }

    protected class ClassFqNameToSourceMap(storageFile: File) : BasicStringMap<String>(storageFile, EnumeratorStringDescriptor(), PathStringDescriptor) {
        operator fun set(fqName: FqName, sourceFile: File) {
            storage[fqName.asString()] = sourceFile.canonicalPath
        }

        operator fun get(fqName: FqName): File? =
                storage[fqName.asString()]?.let(::File)

        fun remove(fqName: FqName) {
            storage.remove(fqName.asString())
        }

        override fun dumpValue(value: String) = value
    }
}