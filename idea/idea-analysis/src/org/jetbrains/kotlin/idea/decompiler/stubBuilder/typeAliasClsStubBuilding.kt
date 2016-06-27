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

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.flags.VISIBILITY
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer

fun createTypeAliasStub(
        parent: StubElement<out PsiElement>,
        typeAliasProto: ProtoBuf.TypeAlias,
        protoContainer: ProtoContainer,
        context: ClsStubBuilderContext
) {
    val shortName = context.nameResolver.getName(typeAliasProto.name)

    val classId = when (protoContainer) {
        is ProtoContainer.Class -> protoContainer.classId.createNestedClassId(shortName)
        is ProtoContainer.Package -> ClassId.topLevel(protoContainer.fqName.child(shortName))
    }

    val typeAlias =
        KotlinTypeAliasStubImpl(
                parent, classId.shortClassName.ref(), classId.asSingleFqName().ref(),
                isTopLevel = !classId.isNestedClass)

    val modifierList = createModifierListStubForDeclaration(typeAlias, typeAliasProto.flags, arrayListOf(VISIBILITY), listOf())

    val typeStubBuilder = TypeClsStubBuilder(context)
    val restConstraints = typeStubBuilder.createTypeParameterListStub(typeAlias, typeAliasProto.typeParameterList)
    assert(restConstraints.isEmpty()) {
        "'where' constraints are not allowed for type aliases"
    }

    if (Flags.HAS_ANNOTATIONS.get(typeAliasProto.flags)) {
        createAnnotationStubs(typeAliasProto.annotationList.map { context.nameResolver.getClassId(it.id) }, modifierList)
    }

    typeStubBuilder.createTypeReferenceStub(typeAlias, typeAliasProto.underlyingType)
}
