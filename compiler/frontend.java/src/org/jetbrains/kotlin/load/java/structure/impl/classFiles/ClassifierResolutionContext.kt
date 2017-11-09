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

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import com.intellij.util.containers.ContainerUtil
import gnu.trove.THashMap
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

typealias ClassIdToJavaClass = (ClassId) -> JavaClass?

class ClassifierResolutionContext private constructor(
        private val classesByQName: ClassIdToJavaClass,
            // Note that this data is fully mutable and its correctness is based on the assumption
            // that nobody starts resolving classifier until type parameters and inner classes are initialized.
            // Currently it's implemented through laziness in the PlainJavaClassifierType.
        private var typeParameters: MutableMap<String, JavaTypeParameter>?,
        private var innerClasses: MutableMap<String, InnerClassInfo>?
) {
    constructor(classesByQName: ClassIdToJavaClass) : this(classesByQName, null, null)

    internal data class Result(val classifier: JavaClassifier?, val qualifiedName: String)

    private class InnerClassInfo(val outerInternalName: String, val simpleName: String)

    internal fun addInnerClass(innerInternalName: String, outerInternalName: String, simpleName: String) {
        if (innerClasses == null) {
            innerClasses = THashMap()
        }

        innerClasses!!.put(innerInternalName, InnerClassInfo(outerInternalName, simpleName))
    }

    internal fun addTypeParameters(newTypeParameters: Collection<JavaTypeParameter>) {
        if (newTypeParameters.isEmpty()) return
        if (typeParameters == null) {
            typeParameters = THashMap()
        }

        newTypeParameters.associateByTo(typeParameters!!) { it.name.identifier }
    }

    internal fun resolveClass(classId: ClassId) = Result(classesByQName(classId), classId.asSingleFqName().asString())
    internal fun resolveTypeParameter(name: String) = Result(typeParameters?.get(name), name)

    internal fun copyForMember() =
            ClassifierResolutionContext(classesByQName, typeParameters?.let(::THashMap), innerClasses?.let(::THashMap))

    // See com.intellij.psi.impl.compiled.StubBuildingVisitor.createMapping(byte[])
    internal fun mapInternalNameToClassId(internalName: String): ClassId {
        if ('.' in internalName) {
            val parts = internalName.split('.')

            val outerClass = mapInternalNameToClassId(parts[0])
            val nestedParts = parts.subList(1, parts.size)

            return nestedParts.fold(outerClass) { classId, part ->
                classId.createNestedClassId(Name.identifier(part))
            }
        }

        if ('$' in internalName) {
            val innerClassInfo = innerClasses?.get(internalName) ?: return mapInternalNameToClassIdNaively(internalName)
            if (Name.isValidIdentifier(innerClassInfo.simpleName)) {
                val outerClassId = mapInternalNameToClassId(innerClassInfo.outerInternalName)
                return outerClassId.createNestedClassId(Name.identifier(innerClassInfo.simpleName))
            }
        }

        return createClassIdForTopLevel(internalName)
    }

    // See com.intellij.psi.impl.compiled.StubBuildingVisitor.GUESSING_MAPPER
    private fun mapInternalNameToClassIdNaively(internalName: String): ClassId {
        val splitPoints = ContainerUtil.newSmartList<Int>()
        for (p in 0..internalName.length - 1) {
            val c = internalName[p]
            if (c == '$' && p > 0 && internalName[p - 1] != '/' && p < internalName.length - 1 && internalName[p + 1] != '$') {
                splitPoints.add(p)
            }
        }

        if (splitPoints.isNotEmpty()) {
            val substrings = (listOf(-1) + splitPoints).zip(splitPoints + internalName.length).map { (from, to) ->
                internalName.substring(from + 1, to)
            }

            val outerFqName = FqName(substrings[0].replace('/', '.'))
            val packageFqName = outerFqName.parent()
            val relativeName =
                    FqName(outerFqName.shortName().asString() + "." +  substrings.subList(1, substrings.size).joinToString("."))

            return ClassId(packageFqName, relativeName, false)
        }

        return createClassIdForTopLevel(internalName)
    }

    private fun createClassIdForTopLevel(internalName: String) = ClassId.topLevel(FqName(internalName.replace('/', '.')))

    internal fun resolveByInternalName(c: String) = resolveClass(mapInternalNameToClassId(c))
}
