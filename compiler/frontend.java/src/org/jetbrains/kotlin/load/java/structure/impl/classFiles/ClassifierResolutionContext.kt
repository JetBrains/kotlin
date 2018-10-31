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

import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifier
import org.jetbrains.kotlin.load.java.structure.JavaTypeParameter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.javaslang.ImmutableHashMap
import org.jetbrains.kotlin.util.javaslang.ImmutableMap
import org.jetbrains.kotlin.util.javaslang.getOrNull

import org.jetbrains.org.objectweb.asm.Type

typealias ClassIdToJavaClass = (ClassId) -> JavaClass?

class ClassifierResolutionContext private constructor(
    private val classesByQName: ClassIdToJavaClass,
    // Note that this data is fully mutable and its correctness is based on the assumption
    // that nobody starts resolving classifier until type parameters and inner classes are initialized.
    // Currently it's implemented through laziness in the PlainJavaClassifierType.
    private var typeParameters: ImmutableMap<String, JavaTypeParameter>,
    private var innerClasses: ImmutableMap<String, InnerClassInfo>
) {
    constructor(classesByQName: ClassIdToJavaClass) : this(classesByQName, ImmutableHashMap.empty(), ImmutableHashMap.empty())

    internal data class Result(val classifier: JavaClassifier?, val qualifiedName: String)

    private class InnerClassInfo(val outerInternalName: String, val simpleName: String)

    internal fun addInnerClass(innerInternalName: String, outerInternalName: String, simpleName: String) {
        innerClasses = innerClasses.put(innerInternalName, InnerClassInfo(outerInternalName, simpleName))
    }

    internal fun addTypeParameters(newTypeParameters: Collection<JavaTypeParameter>) {
        if (newTypeParameters.isEmpty()) return

        typeParameters =
                newTypeParameters
                    .fold(typeParameters) { acc, typeParameter ->
                        acc.put(typeParameter.name.identifier, typeParameter)
                    }
    }

    private fun resolveClass(classId: ClassId) = Result(classesByQName(classId), classId.asSingleFqName().asString())
    internal fun resolveTypeParameter(name: String) = Result(typeParameters.getOrNull(name), name)

    internal fun copyForMember() = ClassifierResolutionContext(classesByQName, typeParameters, innerClasses)

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
            val innerClassInfo = innerClasses.getOrNull(internalName)
            if (innerClassInfo != null && Name.isValidIdentifier(innerClassInfo.simpleName)) {
                val outerClassId = mapInternalNameToClassId(innerClassInfo.outerInternalName)
                return outerClassId.createNestedClassId(Name.identifier(innerClassInfo.simpleName))
            }
        }

        return ClassId.topLevel(FqName(internalName.replace('/', '.')))
    }

    internal fun resolveByInternalName(c: String): Result = resolveClass(mapInternalNameToClassId(c))

    internal fun mapDescToClassId(desc: String): ClassId = mapInternalNameToClassId(Type.getType(desc).internalName)
}
