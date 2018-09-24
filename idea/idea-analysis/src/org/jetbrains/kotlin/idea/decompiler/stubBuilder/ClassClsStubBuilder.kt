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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isNumberedFunctionClassFqName
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.flags.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.supertypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.stubs.elements.KtClassElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName

fun createClassStub(
        parent: StubElement<out PsiElement>,
        classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
        classId: ClassId,
        source: SourceElement?,
        context: ClsStubBuilderContext
) {
    ClassClsStubBuilder(parent, classProto, nameResolver, classId, source, context).build()
}

private class ClassClsStubBuilder(
        private val parentStub: StubElement<out PsiElement>,
        private val classProto: ProtoBuf.Class,
        nameResolver: NameResolver,
        private val classId: ClassId,
        source: SourceElement?,
        outerContext: ClsStubBuilderContext
) {
    private val thisAsProtoContainer = ProtoContainer.Class(
            classProto, nameResolver, TypeTable(classProto.typeTable), source, outerContext.protoContainer
    )
    private val classKind = thisAsProtoContainer.kind

    private val c = outerContext.child(
            classProto.typeParameterList, classId.shortClassName, nameResolver, thisAsProtoContainer.typeTable, thisAsProtoContainer
    )
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val supertypeIds = run {
        val supertypeIds = classProto.supertypes(c.typeTable).map { c.nameResolver.getClassId(it.className) }
        //empty supertype list if single supertype is Any
        if (supertypeIds.singleOrNull()?.let { KotlinBuiltIns.FQ_NAMES.any == it.asSingleFqName().toUnsafe() } ?: false) {
            listOf()
        }
        else {
            supertypeIds
        }
    }

    private val companionObjectName =
            if (classProto.hasCompanionObjectName()) c.nameResolver.getName(classProto.companionObjectName) else null

    private val classOrObjectStub = createClassOrObjectStubAndModifierListStub()

    fun build() {
        val typeConstraintListData = typeStubBuilder.createTypeParameterListStub(classOrObjectStub, classProto.typeParameterList)
        createConstructorStub()
        createDelegationSpecifierList()
        typeStubBuilder.createTypeConstraintListStub(classOrObjectStub, typeConstraintListData)
        createClassBodyAndMemberStubs()
    }

    private fun createClassOrObjectStubAndModifierListStub(): StubElement<out PsiElement> {
        val classOrObjectStub = doCreateClassOrObjectStub()
        val modifierList = createModifierListForClass(classOrObjectStub)
        if (Flags.HAS_ANNOTATIONS.get(classProto.flags)) {
            createAnnotationStubs(c.components.annotationLoader.loadClassAnnotations(thisAsProtoContainer), modifierList)
        }
        return classOrObjectStub
    }

    private fun createModifierListForClass(parent: StubElement<out PsiElement>): KotlinModifierListStubImpl {
        val relevantFlags = arrayListOf(VISIBILITY)
        relevantFlags.add(EXTERNAL_CLASS)
        if (isClass()) {
            relevantFlags.add(INNER)
            relevantFlags.add(DATA)
            relevantFlags.add(MODALITY)
            relevantFlags.add(INLINE_CLASS)
        }
        val additionalModifiers = when (classKind) {
            ProtoBuf.Class.Kind.ENUM_CLASS -> listOf(KtTokens.ENUM_KEYWORD)
            ProtoBuf.Class.Kind.COMPANION_OBJECT -> listOf(KtTokens.COMPANION_KEYWORD)
            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> listOf(KtTokens.ANNOTATION_KEYWORD)
            else -> listOf<KtModifierKeywordToken>()
        }
        return createModifierListStubForDeclaration(parent, classProto.flags, relevantFlags, additionalModifiers)
    }

    private fun doCreateClassOrObjectStub(): StubElement<out PsiElement> {
        val isCompanionObject = classKind == ProtoBuf.Class.Kind.COMPANION_OBJECT
        val fqName = classId.asSingleFqName()
        val shortName = fqName.shortName().ref()
        val superTypeRefs = supertypeIds.filterNot {
            //TODO: filtering function types should go away
            isNumberedFunctionClassFqName(it.asSingleFqName().toUnsafe())
        }.map { it.shortClassName.ref() }.toTypedArray()
        return when (classKind) {
            ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.COMPANION_OBJECT -> {
                KotlinObjectStubImpl(
                        parentStub, shortName, fqName, superTypeRefs,
                        isTopLevel = !classId.isNestedClass,
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
                        isInterface = classKind == ProtoBuf.Class.Kind.INTERFACE,
                        isEnumEntry = classKind == ProtoBuf.Class.Kind.ENUM_ENTRY,
                        isLocal = false,
                        isTopLevel = !classId.isNestedClass
                )
            }
        }
    }

    private fun createConstructorStub() {
        if (!isClass()) return

        val primaryConstructorProto = classProto.constructorList.find { !Flags.IS_SECONDARY.get(it.flags) } ?: return

        createConstructorStub(classOrObjectStub, primaryConstructorProto, c, thisAsProtoContainer)
    }

    private fun createDelegationSpecifierList() {
        // if single supertype is any then no delegation specifier list is needed
        if (supertypeIds.isEmpty()) return

        val delegationSpecifierListStub =
                KotlinPlaceHolderStubImpl<KtSuperTypeList>(classOrObjectStub, KtStubElementTypes.SUPER_TYPE_LIST)

        classProto.supertypes(c.typeTable).forEach { type ->
            val superClassStub = KotlinPlaceHolderStubImpl<KtSuperTypeEntry>(
                    delegationSpecifierListStub, KtStubElementTypes.SUPER_TYPE_ENTRY
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
        if (classKind != ProtoBuf.Class.Kind.ENUM_CLASS) return

        classProto.enumEntryList.forEach { entry ->
            val name = c.nameResolver.getName(entry.name)
            val annotations = c.components.annotationLoader.loadEnumEntryAnnotations(thisAsProtoContainer, entry)
            val enumEntryStub = KotlinClassStubImpl(
                    KtStubElementTypes.ENUM_ENTRY,
                    classBody,
                    qualifiedName = c.containerFqName.child(name).ref(),
                    name = name.ref(),
                    superNames = arrayOf(),
                    isInterface = false,
                    isEnumEntry = true,
                    isLocal = false,
                    isTopLevel = false
            )
            if (annotations.isNotEmpty()) {
                createAnnotationStubs(annotations, createEmptyModifierListStub(enumEntryStub))
            }
        }
    }

    private fun createCallableMemberStubs(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        for (secondaryConstructorProto in classProto.constructorList) {
            if (Flags.IS_SECONDARY.get(secondaryConstructorProto.flags)) {
                createConstructorStub(classBody, secondaryConstructorProto, c, thisAsProtoContainer)
            }
        }

        createDeclarationsStubs(
                classBody, c, thisAsProtoContainer, classProto.functionList, classProto.propertyList, classProto.typeAliasList)
    }

    private fun isClass(): Boolean {
        return classKind == ProtoBuf.Class.Kind.CLASS ||
               classKind == ProtoBuf.Class.Kind.ENUM_CLASS ||
               classKind == ProtoBuf.Class.Kind.ANNOTATION_CLASS
    }

    private fun createInnerAndNestedClasses(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        classProto.nestedClassNameList.forEach { id ->
            val nestedClassName = c.nameResolver.getName(id)
            if (nestedClassName != companionObjectName) {
                val nestedClassId = classId.createNestedClassId(nestedClassName)
                createNestedClassStub(classBody, nestedClassId)
            }
        }
    }

    private fun createNestedClassStub(classBody: StubElement<out PsiElement>, nestedClassId: ClassId) {
        val (nameResolver, classProto, _, sourceElement) =
                c.components.classDataFinder.findClassData(nestedClassId)
                        ?: c.components.virtualFileForDebug.let { rootFile ->
                            LOG.error(
                                "Could not find class data for nested class $nestedClassId of class ${nestedClassId.outerClassId}\n" +
                                        "Root file: ${rootFile.canonicalPath}\n" +
                                        "Dir: ${rootFile.parent.canonicalPath}\n" +
                                        "Children:\n" +
                                        rootFile.parent.children.sortedBy { it.name }.joinToString(separator = "\n") {
                                            "${it.name} (valid: ${it.isValid})"
                                        }
                            )
                            return
                        }
        createClassStub(classBody, classProto, nameResolver, nestedClassId, sourceElement, c)
    }

    companion object {
        private val LOG = Logger.getInstance(ClassClsStubBuilder::class.java)
    }
}
