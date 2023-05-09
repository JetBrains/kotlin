// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.VISIBILITY
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.underlyingType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getName

fun createTypeAliasStub(
    parent: StubElement<out PsiElement>,
    typeAliasProto: ProtoBuf.TypeAlias,
    protoContainer: ProtoContainer,
    outerContext: ClsStubBuilderContext
) {
    val c = outerContext.child(typeAliasProto.typeParameterList)
    val shortName = c.nameResolver.getName(typeAliasProto.name)

    val classId = when (protoContainer) {
        is ProtoContainer.Class -> protoContainer.classId.createNestedClassId(shortName)
        is ProtoContainer.Package -> ClassId.topLevel(protoContainer.fqName.child(shortName))
    }

    val typeAlias = KotlinTypeAliasStubImpl(
        parent, classId.shortClassName.ref(), classId.asSingleFqName().ref(), classId,
        isTopLevel = !classId.isNestedClass
    )

    val modifierList = createModifierListStubForDeclaration(typeAlias, typeAliasProto.flags, arrayListOf(VISIBILITY), listOf())

    val typeStubBuilder = TypeClsStubBuilder(c)
    val restConstraints = typeStubBuilder.createTypeParameterListStub(typeAlias, typeAliasProto.typeParameterList)
    assert(restConstraints.isEmpty()) {
        "'where' constraints are not allowed for type aliases"
    }

    if (Flags.HAS_ANNOTATIONS.get(typeAliasProto.flags)) {
        createAnnotationStubs(
            typeAliasProto.annotationList.map {
                c.components.annotationLoader.loadAnnotation(it, c.nameResolver)
            },
            modifierList
        )
    }

    val typeAliasUnderlyingType = typeAliasProto.underlyingType(c.typeTable)
    typeStubBuilder.createTypeReferenceStub(typeAlias, typeAliasUnderlyingType)
}
