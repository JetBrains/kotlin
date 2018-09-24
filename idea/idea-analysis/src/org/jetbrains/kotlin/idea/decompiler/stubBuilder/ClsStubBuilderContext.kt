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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.AnnotationAndConstantLoader
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getName

data class ClassIdWithTarget(val classId: ClassId, val target: AnnotationUseSiteTarget?)

class ClsStubBuilderComponents(
        val classDataFinder: ClassDataFinder,
        val annotationLoader: AnnotationAndConstantLoader<ClassId, Unit>,
        val virtualFileForDebug: VirtualFile
) {
    fun createContext(
            nameResolver: NameResolver,
            packageFqName: FqName,
            typeTable: TypeTable
    ): ClsStubBuilderContext {
        return ClsStubBuilderContext(this, nameResolver, packageFqName, EmptyTypeParameters, typeTable, protoContainer = null)
    }
}

interface TypeParameters {
    operator fun get(id: Int): Name

    fun child(nameResolver: NameResolver, innerTypeParameters: List<ProtoBuf.TypeParameter>)
            = TypeParametersImpl(nameResolver, innerTypeParameters, parent = this)
}

object EmptyTypeParameters : TypeParameters {
    override fun get(id: Int): Name = throw IllegalStateException("Unknown type parameter with id = $id")
}

class TypeParametersImpl(
        nameResolver: NameResolver,
        typeParameterProtos: Collection<ProtoBuf.TypeParameter>,
        private val parent: TypeParameters
) : TypeParameters {
    private val typeParametersById = typeParameterProtos.map { Pair(it.id, nameResolver.getName(it.name)) }.toMap()

    override fun get(id: Int): Name = typeParametersById[id] ?: parent[id]
}

class ClsStubBuilderContext(
        val components: ClsStubBuilderComponents,
        val nameResolver: NameResolver,
        val containerFqName: FqName,
        val typeParameters: TypeParameters,
        val typeTable: TypeTable,
        val protoContainer: ProtoContainer.Class?
)

internal fun ClsStubBuilderContext.child(
        typeParameterList: List<ProtoBuf.TypeParameter>,
        name: Name? = null,
        nameResolver: NameResolver = this.nameResolver,
        typeTable: TypeTable = this.typeTable,
        protoContainer: ProtoContainer.Class? = this.protoContainer
): ClsStubBuilderContext {
    return ClsStubBuilderContext(
            this.components,
            nameResolver,
            if (name != null) this.containerFqName.child(name) else this.containerFqName,
            this.typeParameters.child(nameResolver, typeParameterList),
            typeTable,
            protoContainer
    )
}
