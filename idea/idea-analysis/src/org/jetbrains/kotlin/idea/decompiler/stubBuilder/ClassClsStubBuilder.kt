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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.FlagsToModifiers.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtDelegationSpecifierList
import org.jetbrains.kotlin.psi.KtDelegatorToSuperClass
import org.jetbrains.kotlin.psi.stubs.elements.KtClassElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.deserialization.supertypes
import org.jetbrains.kotlin.utils.sure

fun createClassStub(parent: StubElement<out PsiElement>, classProto: ProtoBuf.Class, classId: ClassId, context: ClsStubBuilderContext) {
    ClassClsStubBuilder(parent, classProto, classId, context).build()
}

private class ClassClsStubBuilder(
        private val parentStub: StubElement<out PsiElement>,
        private val classProto: ProtoBuf.Class,
        private val classId: ClassId,
        private val outerContext: ClsStubBuilderContext
) {
    private val c = outerContext.child(classProto.typeParameterList, classId.shortClassName, TypeTable(classProto.typeTable))
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val classKind = Flags.CLASS_KIND[classProto.flags]
    private val supertypeIds = run {
        val supertypeIds = classProto.supertypes(c.typeTable).map { c.nameResolver.getClassId(it.className) }
        //empty supertype list if single supertype is Any
        if (supertypeIds.singleOrNull()?.let { KotlinBuiltIns.isAny(it.asSingleFqName().toUnsafe()) } ?: false) {
            listOf()
        }
        else {
            supertypeIds
        }
    }
    private val companionObjectName = if (classProto.hasCompanionObjectName()) c.nameResolver.getName(classProto.getCompanionObjectName()) else null

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
            relevantFlags.add(DATA)
            relevantFlags.add(MODALITY)
        }
        val additionalModifiers = when (classKind) {
            ProtoBuf.Class.Kind.ENUM_CLASS -> listOf(KtTokens.ENUM_KEYWORD)
            ProtoBuf.Class.Kind.COMPANION_OBJECT -> listOf(KtTokens.COMPANION_KEYWORD)
            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> listOf(KtTokens.ANNOTATION_KEYWORD)
            else -> listOf<KtModifierKeywordToken>()
        }
        return createModifierListStubForDeclaration(parent, classProto.getFlags(), relevantFlags, additionalModifiers)
    }

    private fun doCreateClassOrObjectStub(): StubElement<out PsiElement> {
        val isCompanionObject = classKind == ProtoBuf.Class.Kind.COMPANION_OBJECT
        val fqName = outerContext.containerFqName.child(classId.getShortClassName())
        val shortName = fqName.shortName().ref()
        val superTypeRefs = supertypeIds.filterNot {
            //TODO: filtering function types should go away
            KotlinBuiltIns.isNumberedFunctionClassFqName(it.asSingleFqName().toUnsafe())
        }.map { it.getShortClassName().ref() }.toTypedArray()
        return when (classKind) {
            ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.COMPANION_OBJECT -> {
                KotlinObjectStubImpl(
                        parentStub, shortName, fqName, superTypeRefs,
                        isTopLevel = !classId.isNestedClass(),
                        isDefault = isCompanionObject,
                        isLocal = false,
                        isObjectLiteral = false
                )
            }
            else -> {
                KotlinClassStubImpl(
                        KtClassElementType.getStubType(classKind == ProtoBuf.Class.Kind.ENUM_ENTRY),
                        parentStub,
                        fqName.ref(),
                        shortName,
                        superTypeRefs,
                        isTrait = classKind == ProtoBuf.Class.Kind.INTERFACE,
                        isEnumEntry = classKind == ProtoBuf.Class.Kind.ENUM_ENTRY,
                        isLocal = false,
                        isTopLevel = !classId.isNestedClass()
                )
            }
        }
    }

    private fun createConstructorStub() {
        if (!isClass()) return

        val primaryConstructorProto = classProto.constructorList.find { !Flags.IS_SECONDARY.get(it.flags) } ?: return

        createConstructorStub(classOrObjectStub, primaryConstructorProto, c, ProtoContainer(classProto, null, c.nameResolver, c.typeTable))
    }

    private fun createDelegationSpecifierList() {
        // if single supertype is any then no delegation specifier list is needed
        if (supertypeIds.isEmpty()) return

        val delegationSpecifierListStub =
                KotlinPlaceHolderStubImpl<KtDelegationSpecifierList>(classOrObjectStub, KtStubElementTypes.DELEGATION_SPECIFIER_LIST)

        classProto.supertypes(c.typeTable).forEach { type ->
            val superClassStub = KotlinPlaceHolderStubImpl<KtDelegatorToSuperClass>(
                    delegationSpecifierListStub, KtStubElementTypes.DELEGATOR_SUPER_CLASS
            )
            typeStubBuilder.createTypeReferenceStub(superClassStub, type)
        }
    }

    private fun createClassBodyAndMemberStubs() {
        val classBody = KotlinPlaceHolderStubImpl<KtClassBody>(classOrObjectStub, KtStubElementTypes.CLASS_BODY)
        createEnumEntryStubs(classBody)
        createCompanionObjectStub(classBody)
        createCallableMemberStubs(classBody)
        createInnerAndNestedClasses(classBody)
    }

    private fun createCompanionObjectStub(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        if (companionObjectName == null) {
            return
        }

        val companionObjectId = classId.createNestedClassId(companionObjectName)
        createNestedClassStub(classBody, companionObjectId)
    }

    private fun createEnumEntryStubs(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        classProto.getEnumEntryList().forEach { id ->
            val name = c.nameResolver.getName(id)
            KotlinClassStubImpl(
                    KtStubElementTypes.ENUM_ENTRY,
                    classBody,
                    qualifiedName = c.containerFqName.child(name).ref(),
                    name = name.ref(),
                    superNames = arrayOf(),
                    isTrait = false,
                    isEnumEntry = true,
                    isLocal = false,
                    isTopLevel = false
            )
        }
    }

    private fun createCallableMemberStubs(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        val container = ProtoContainer(classProto, null, c.nameResolver, c.typeTable)

        for (secondaryConstructorProto in classProto.constructorList) {
            if (Flags.IS_SECONDARY.get(secondaryConstructorProto.flags)) {
                createConstructorStub(classBody, secondaryConstructorProto, c, container)
            }
        }

        createCallableStubs(classBody, c, container, classProto.functionList, classProto.propertyList)
    }

    private fun isClass(): Boolean {
        return classKind == ProtoBuf.Class.Kind.CLASS ||
               classKind == ProtoBuf.Class.Kind.ENUM_CLASS ||
               classKind == ProtoBuf.Class.Kind.ANNOTATION_CLASS
    }

    private fun createInnerAndNestedClasses(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        classProto.getNestedClassNameList().forEach { id ->
            val nestedClassName = c.nameResolver.getName(id)
            if (nestedClassName != companionObjectName) {
                val nestedClassId = classId.createNestedClassId(nestedClassName)
                createNestedClassStub(classBody, nestedClassId)
            }
        }
    }

    private fun createNestedClassStub(classBody: StubElement<out PsiElement>, nestedClassId: ClassId) {
        val classDataWithSource = c.components.classDataFinder.findClassData(nestedClassId).sure {
            "Could not find class data for nested class ${nestedClassId.shortClassName} of class ${nestedClassId.outerClassId}"
        }
        val (nameResolver, classProto) = classDataWithSource.classData
        createClassStub(classBody, classProto, nestedClassId, c.child(nameResolver, TypeTable(classProto.typeTable)))
    }
}
