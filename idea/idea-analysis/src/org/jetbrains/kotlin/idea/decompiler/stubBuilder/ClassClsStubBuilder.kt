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

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.psi.stubs.elements.JetClassElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.JetClassBody
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.kotlin.psi.JetParameterList
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.JetDelegationSpecifierList
import org.jetbrains.kotlin.psi.JetDelegatorToSuperClass
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.serialization.ProtoBuf.Type
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.FlagsToModifiers.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns


fun createClassStub(parent: StubElement<out PsiElement>, classProto: ProtoBuf.Class, classId: ClassId, context: ClsStubBuilderContext) {
    ClassClsStubBuilder(parent, classProto, classId, context).build()
}

private class ClassClsStubBuilder(
        private val parentStub: StubElement<out PsiElement>,
        private val classProto: ProtoBuf.Class,
        private val classId: ClassId,
        private val outerContext: ClsStubBuilderContext
) {
    private val c = outerContext.child(classProto.getTypeParameterList(), classId.getRelativeClassName().shortName())
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val classKind = Flags.CLASS_KIND[classProto.getFlags()]
    private val supertypeIds = classProto.getSupertypeList().map {
        type ->
        assert(type.getConstructor().getKind() == Type.Constructor.Kind.CLASS)
        c.nameResolver.getClassId(type.getConstructor().getId())
    }.let {
        supertypeIds ->
        //empty supertype list if single supertype is Any
        if (supertypeIds.singleOrNull()?.let { KotlinBuiltIns.isAny(it.asSingleFqName()) } ?: false) {
            listOf()
        }
        else {
            supertypeIds
        }
    }
    private val defaultObjectName = if (classProto.hasDefaultObjectName()) c.nameResolver.getName(classProto.getDefaultObjectName()) else null

    private val classOrObjectStub = createClassOrObjectStubAndModifierListStub()

    fun build() {
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(classOrObjectStub, classProto.getTypeParameterList())
        createConstructorStub()
        createDelegationSpecifierList()
        typeStubBuilder.createTypeConstraintListStub(classOrObjectStub, typeConstraintListData)
        createClassBodyAndMemberStubs()
    }

    private fun createClassOrObjectStubAndModifierListStub(): StubElement<out PsiElement> {
        val classOrObjectStub = doCreateClassOrObjectStub()
        val modifierList = createModifierListForClass(classOrObjectStub)
        createAnnotationStubs(c.components.annotationLoader.loadClassAnnotations(classProto, c.nameResolver), modifierList)
        return classOrObjectStub
    }

    private fun createModifierListForClass(parent: StubElement<out PsiElement>): KotlinModifierListStubImpl {
        val relevantFlags = arrayListOf(VISIBILITY)
        if (isClass()) {
            relevantFlags.add(INNER)
            relevantFlags.add(MODALITY)
        }
        val additionalModifiers = when (classKind) {
            ProtoBuf.Class.Kind.ENUM_CLASS -> listOf(JetTokens.ENUM_KEYWORD)
            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> listOf(JetTokens.ANNOTATION_KEYWORD)
            ProtoBuf.Class.Kind.CLASS_OBJECT -> listOf(JetTokens.DEFAULT_KEYWORD)
            else -> listOf<JetModifierKeywordToken>()
        }
        return createModifierListStubForDeclaration(parent, classProto.getFlags(), relevantFlags, additionalModifiers)
    }

    private fun doCreateClassOrObjectStub(): StubElement<out PsiElement> {
        val isDefaultObject = classKind == ProtoBuf.Class.Kind.CLASS_OBJECT
        val fqName = outerContext.containerFqName.child(classId.getRelativeClassName().shortName())
        val shortName = fqName.shortName()?.ref()
        val superTypeRefs = supertypeIds.filter {
            //TODO: filtering function types should go away
            !KotlinBuiltIns.isExactFunctionType(it.asSingleFqName()) && !KotlinBuiltIns.isExactExtensionFunctionType(it.asSingleFqName())
        }.map { it.getRelativeClassName().shortName().ref() }.copyToArray()
        return when (classKind) {
            ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.CLASS_OBJECT -> {
                KotlinObjectStubImpl(
                        parentStub, shortName, fqName, superTypeRefs,
                        isTopLevel = !classId.isNestedClass(),
                        isDefault = isDefaultObject,
                        isLocal = false,
                        isObjectLiteral = false
                )
            }
            else -> {
                KotlinClassStubImpl(
                        JetClassElementType.getStubType(classKind == ProtoBuf.Class.Kind.ENUM_ENTRY),
                        parentStub,
                        fqName.ref(),
                        shortName,
                        superTypeRefs,
                        isTrait = classKind == ProtoBuf.Class.Kind.TRAIT,
                        isEnumEntry = classKind == ProtoBuf.Class.Kind.ENUM_ENTRY,
                        isLocal = false,
                        isTopLevel = !classId.isNestedClass()
                )
            }
        }
    }

    private fun createConstructorStub() {
        if (!isClass()) return

        val primaryConstructorProto = classProto.getPrimaryConstructor()
        if (primaryConstructorProto.hasData()) {
            typeStubBuilder.createValueParameterListStub(classOrObjectStub, primaryConstructorProto.getData(), ProtoContainer(classProto, null))
        }
        else {
            //default empty constructor
            KotlinPlaceHolderStubImpl<JetParameterList>(classOrObjectStub, JetStubElementTypes.VALUE_PARAMETER_LIST)
        }
    }

    private fun createDelegationSpecifierList() {
        // if single supertype is any then no delegation specifier list is needed
        if (supertypeIds.isEmpty()) return

        val delegationSpecifierListStub =
                KotlinPlaceHolderStubImpl<JetDelegationSpecifierList>(classOrObjectStub, JetStubElementTypes.DELEGATION_SPECIFIER_LIST)

        classProto.getSupertypeList().forEach { type ->
            val superClassStub = KotlinPlaceHolderStubImpl<JetDelegatorToSuperClass>(
                    delegationSpecifierListStub, JetStubElementTypes.DELEGATOR_SUPER_CLASS
            )
            typeStubBuilder.createTypeReferenceStub(superClassStub, type)
        }
    }

    private fun createClassBodyAndMemberStubs() {
        val classBody = KotlinPlaceHolderStubImpl<JetClassBody>(classOrObjectStub, JetStubElementTypes.CLASS_BODY)
        createDefaultObjectStub(classBody)
        createEnumEntryStubs(classBody)
        createCallableMemberStubs(classBody)
        createInnerAndNestedClasses(classBody)
    }

    private fun createDefaultObjectStub(classBody: KotlinPlaceHolderStubImpl<JetClassBody>) {
        if (defaultObjectName == null) {
            return
        }

        val defaultObjectId = classId.createNestedClassId(defaultObjectName)
        createNestedClassStub(classBody, defaultObjectId)
    }

    private fun createEnumEntryStubs(classBody: KotlinPlaceHolderStubImpl<JetClassBody>) {
        classProto.getEnumEntryList().forEach { id ->
            val name = c.nameResolver.getName(id)
            KotlinClassStubImpl(
                    JetStubElementTypes.ENUM_ENTRY,
                    classBody,
                    qualifiedName = c.containerFqName.child(name).ref(),
                    name = name.ref(),
                    superNames = array(),
                    isTrait = false,
                    isEnumEntry = true,
                    isLocal = false,
                    isTopLevel = false
            )
        }
    }

    private fun createCallableMemberStubs(classBody: KotlinPlaceHolderStubImpl<JetClassBody>) {
        val container = ProtoContainer(classProto, null)
        for (callableProto in classProto.getMemberList()) {
            createCallableStub(classBody, callableProto, c, container)
        }
    }

    private fun isClass(): Boolean {
        return classKind == ProtoBuf.Class.Kind.CLASS ||
               classKind == ProtoBuf.Class.Kind.ENUM_CLASS ||
               classKind == ProtoBuf.Class.Kind.ANNOTATION_CLASS
    }

    private fun createInnerAndNestedClasses(classBody: KotlinPlaceHolderStubImpl<JetClassBody>) {
        classProto.getNestedClassNameList().forEach { id ->
            val nestedClassName = c.nameResolver.getName(id)
            if (nestedClassName != defaultObjectName) {
                val nestedClassId = classId.createNestedClassId(nestedClassName)
                createNestedClassStub(classBody, nestedClassId)
            }
        }
    }

    private fun createNestedClassStub(classBody: StubElement<out PsiElement>, nestedClassId: ClassId) {
        val classData = c.components.classDataFinder.findClassData(nestedClassId)!!
        createClassStub(classBody, classData.getClassProto(), nestedClassId, c.child(classData.getNameResolver()))
    }
}
