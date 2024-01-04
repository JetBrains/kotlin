// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isNumberedFunctionClassFqName
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
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
    private val contextReceiversListStubBuilder = ContextReceiversListStubBuilder(c)
    private val typeStubBuilder = TypeClsStubBuilder(c)
    private val supertypeIds = run {
        val supertypeIds = classProto.supertypes(c.typeTable).map { c.nameResolver.getClassId(it.className) }
        //empty supertype list if single supertype is Any
        if (supertypeIds.singleOrNull()?.let { StandardNames.FqNames.any == it.asSingleFqName().toUnsafe() } == true) {
            listOf()
        } else {
            supertypeIds
        }
    }

    private val companionObjectName = if (classProto.hasCompanionObjectName())
        c.nameResolver.getName(classProto.companionObjectName)
    else
        null

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
        contextReceiversListStubBuilder.createContextReceiverStubs(classOrObjectStub, classProto.contextReceiverTypes(c.typeTable))
        val modifierList = createModifierListForClass(classOrObjectStub)
        if (Flags.HAS_ANNOTATIONS.get(classProto.flags)) {
            createAnnotationStubs(c.components.annotationLoader.loadClassAnnotations(thisAsProtoContainer), modifierList)
        }
        return classOrObjectStub
    }

    private fun createModifierListForClass(parent: StubElement<out PsiElement>): KotlinModifierListStubImpl {
        val relevantFlags = arrayListOf(VISIBILITY)
        relevantFlags.add(EXTERNAL_CLASS)
        relevantFlags.add(EXPECT_CLASS)
        if (isClass()) {
            relevantFlags.add(INNER)
            relevantFlags.add(DATA)
            relevantFlags.add(MODALITY)
            relevantFlags.add(VALUE_CLASS)
        }
        if (isInterface()) {
            relevantFlags.add(FUN_INTERFACE)
            relevantFlags.add(INTERFACE_MODALITY)
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
        val classId = classId.takeUnless { it.isLocal }
        return when (classKind) {
            ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.COMPANION_OBJECT -> {
                KotlinObjectStubImpl(
                    parentStub, shortName, fqName,
                    classId = classId,
                    superTypeRefs,
                    isTopLevel = !this.classId.isNestedClass,
                    isDefault = isCompanionObject,
                    isLocal = false,
                    isObjectLiteral = false,
                )
            }
            else -> {
                KotlinClassStubImpl(
                    KtClassElementType.getStubType(classKind == ProtoBuf.Class.Kind.ENUM_ENTRY),
                    parentStub,
                    fqName.ref(),
                    classId = classId,
                    shortName,
                    superTypeRefs,
                    isInterface = classKind == ProtoBuf.Class.Kind.INTERFACE,
                    isEnumEntry = classKind == ProtoBuf.Class.Kind.ENUM_ENTRY,
                    isLocal = false,
                    isTopLevel = !this.classId.isNestedClass,
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

        val delegationSpecifierListStub = KotlinPlaceHolderStubImpl<KtSuperTypeList>(classOrObjectStub, KtStubElementTypes.SUPER_TYPE_LIST)

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
        createTypeAliasesStubs(classBody)
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
                classId = null, // enum entry do not have class id
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

        createDeclarationsStubs(classBody, c, thisAsProtoContainer, classProto.functionList, classProto.propertyList)
    }

    private fun isClass(): Boolean {
        return classKind == ProtoBuf.Class.Kind.CLASS ||
                classKind == ProtoBuf.Class.Kind.ENUM_CLASS ||
                classKind == ProtoBuf.Class.Kind.ANNOTATION_CLASS
    }

    private fun isInterface(): Boolean {
        return classKind == ProtoBuf.Class.Kind.INTERFACE
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

    private fun createTypeAliasesStubs(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        createTypeAliasesStubs(classBody, c, thisAsProtoContainer, classProto.typeAliasList)
    }

    private fun createNestedClassStub(classBody: StubElement<out PsiElement>, nestedClassId: ClassId) {
        val (nameResolver, classProto, _, sourceElement) =
            c.components.classDataFinder.findClassData(nestedClassId)
                ?: c.components.virtualFileForDebug.let { rootFile ->
                    if (LOG.isDebugEnabled) {
                        val outerClassId = nestedClassId.outerClassId
                        val sortedChildren = rootFile.parent.children.sortedBy { it.name }
                        val msgPrefix = "Could not find data for nested class $nestedClassId of class $outerClassId\n"
                        val explanation = when {
                            outerClassId != null && sortedChildren.none { it.name.startsWith("${outerClassId.relativeClassName}\$a") } ->
                                // KT-29427: case with obfuscation
                                "Reason: obfuscation suspected (single-letter name)\n"
                            else ->
                                // General case
                                ""
                        }
                        val msg = msgPrefix + explanation +
                                "Root file: ${rootFile.canonicalPath}\n" +
                                "Dir: ${rootFile.parent.canonicalPath}\n" +
                                "Children:\n" +
                                sortedChildren.joinToString(separator = "\n") {
                                    "${it.name} (valid: ${it.isValid})"
                                }
                        LOG.debug(msg)
                    }
                    return
                }
        createClassStub(classBody, classProto, nameResolver, nestedClassId, sourceElement, c)
    }

    companion object {
        private val LOG = Logger.getInstance(ClassClsStubBuilder::class.java)
    }
}
