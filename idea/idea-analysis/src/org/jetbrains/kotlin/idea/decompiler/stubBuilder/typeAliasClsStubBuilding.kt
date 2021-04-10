/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.flags.VISIBILITY
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.underlyingType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName

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

    val typeAlias = KotlinTypeAliasStubImpl(
        parent, classId.shortClassName.ref(), classId.asSingleFqName().ref(), classId,
        isTopLevel = !classId.isNestedClass
    )

    val modifierList = createModifierListStubForDeclaration(typeAlias, typeAliasProto.flags, arrayListOf(VISIBILITY), listOf())

    val typeStubBuilder = TypeClsStubBuilder(context)
    val restConstraints = typeStubBuilder.createTypeParameterListStub(typeAlias, typeAliasProto.typeParameterList)
    assert(restConstraints.isEmpty()) {
        "'where' constraints are not allowed for type aliases"
    }

    if (Flags.HAS_ANNOTATIONS.get(typeAliasProto.flags)) {
        createAnnotationStubs(typeAliasProto.annotationList.map { context.nameResolver.getClassId(it.id) }, modifierList)
    }

    val typeAliasUnderlyingType = typeAliasProto.underlyingType(context.typeTable)
    typeStubBuilder.createTypeReferenceStub(typeAlias, typeAliasUnderlyingType)
}
