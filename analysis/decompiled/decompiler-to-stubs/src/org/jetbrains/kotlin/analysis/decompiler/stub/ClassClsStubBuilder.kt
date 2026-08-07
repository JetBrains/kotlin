/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.analysis.decompiler.stub.flags.*
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.isNumberedFunctionClassFqName
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.serialization.deserialization.ProtoContainer
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

fun createClassStub(
    parent: StubElement<out PsiElement>,
    classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    classId: ClassId,
    source: SourceElement?,
    context: ClsStubBuilderContext,
) {
    ProgressManager.checkCanceled()

    ClassClsStubBuilder(parent, classProto, nameResolver, classId, source, context).build()
}

private class ClassClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val classProto: ProtoBuf.Class,
    nameResolver: NameResolver,
    private val classId: ClassId,
    source: SourceElement?,
    outerContext: ClsStubBuilderContext,
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

    private val primaryConstructorProto = classProto.constructorList.find { !Flags.IS_SECONDARY.get(it.flags) }

    /**
     * The properties written as a `val` parameter of the primary constructor instead of a member, keyed by their name.
     *
     * The compiler forbids any member but a constructor parameter in an annotation class, so each of its properties
     * is one of those parameters, and the pair belongs together exactly as it is spelled in the sources.
     */
    private val foldedProperties: Map<Name, ProtoBuf.Property> = computeFoldedProperties()

    private val memberPropertyProtos = classProto.propertyList.filterNot { c.nameResolver.getName(it.name) in foldedProperties }

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
        createAnnotationStubs(c.components.annotationLoader.loadClassAnnotations(thisAsProtoContainer), modifierList)
        typeStubBuilder.createContextReceiverStubs(modifierList, classProto.contextReceiverTypes(c.typeTable))
        return classOrObjectStub
    }

    private fun createModifierListForClass(parent: StubElement<out PsiElement>): KotlinModifierListStubImpl {
        val relevantFlags = arrayListOf(
            VISIBILITY,
            EXTERNAL_CLASS,
            EXPECT_CLASS,
            INNER,
            DATA,
            VALUE_CLASS,
            FUN_INTERFACE,
        )

        when {
            isInterface() -> relevantFlags.add(INTERFACE_MODALITY)
            isObject() -> {} // objects are final always
            else -> relevantFlags.add(MODALITY)
        }

        val additionalModifiers = when (classKind) {
            ProtoBuf.Class.Kind.ENUM_CLASS -> listOf(KtTokens.ENUM_KEYWORD)
            ProtoBuf.Class.Kind.COMPANION_OBJECT -> listOf(KtTokens.COMPANION_KEYWORD)
            ProtoBuf.Class.Kind.ANNOTATION_CLASS -> listOf(KtTokens.ANNOTATION_KEYWORD)
            else -> emptyList()
        }

        return createModifierListStubForDeclaration(
            parent,
            classProto.flags,
            relevantFlags,
            additionalModifiers,
            returnValueStatus = null,
        )
    }

    @OptIn(KtImplementationDetail::class)
    private fun doCreateClassOrObjectStub(): StubElement<out PsiElement> {
        val fqName = classId.asSingleFqName()
        val shortName = fqName.shortName().ref()
        val superTypeRefs = supertypeIds.filterNot {
            //TODO: filtering function types should go away
            isNumberedFunctionClassFqName(it.asSingleFqName().toUnsafe())
        }.map { it.shortClassName.ref() }.ifNotEmpty { toTypedArray() } ?: StringRef.EMPTY_ARRAY

        val kdoc = classProto.getExtensionOrNull(KlibMetadataProtoBuf.classKdoc)

        @OptIn(ClassIdBasedLocality::class)
        val classId = classId.takeUnless { it.isLocal }
        val isTopLevel = classId?.isNestedClass == false
        return when (classKind) {
            ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.COMPANION_OBJECT -> {
                KotlinObjectStubImpl(
                    parentStub, shortName, fqName,
                    classId = classId,
                    superTypeRefs,
                    isTopLevel = isTopLevel,
                    isLocal = false,
                    isObjectLiteral = false,
                    kdocText = kdoc,
                )
            }

            ProtoBuf.Class.Kind.ENUM_ENTRY -> error("Enum entries have to be created as members via '${::createEnumEntryStubs.name}'")

            else -> {
                KotlinClassStubImpl(
                    parent = parentStub,
                    qualifiedName = fqName.ref(),
                    classId = classId,
                    name = shortName,
                    superNameRefs = superTypeRefs,
                    isInterface = classKind == ProtoBuf.Class.Kind.INTERFACE,
                    isClsStubCompiledToJvmDefaultImplementation = JvmProtoBufUtil.isNewPlaceForBodyGeneration(classProto),
                    isLocal = false,
                    isTopLevel = isTopLevel,
                    kdocText = kdoc,
                    valueClassRepresentation = createValueClassRepresentation(),
                )
            }
        }
    }

    /**
     * Returns the kind of a value class together with its underlying properties, or `null` when the class is not a value class, or when its
     * representation cannot be restored from the metadata at hand.
     *
     * A value object needs no representation of its own: it declares no underlying properties, and the `value` modifier in its modifier
     * list is enough to restore the representation on the fly.
     *
     * @see org.jetbrains.kotlin.serialization.deserialization.loadValueClassRepresentation
     */
    @OptIn(KtImplementationDetail::class)
    private fun createValueClassRepresentation(): KotlinValueClassRepresentation? = when {
        // An inline class is the only kind of value class which names its underlying property in the class itself
        classProto.hasInlineClassUnderlyingPropertyName() -> createInlineClassRepresentation()

        // A full value class is marked with the class flag only, so its underlying properties have to be looked up
        Flags.IS_VALUE_CLASS.get(classProto.flags) && !hasJvmInlineAnnotation() -> createFullValueClassRepresentation()

        // Either not a value class at all, or a multi-field '@JvmInline' value class from an experimental compiler version.
        // The latter has no representation anymore, exactly as in the compiler's own deserialization.
        else -> null
    }

    @OptIn(KtImplementationDetail::class)
    private fun createInlineClassRepresentation(): KotlinInlineClassRepresentation? {
        val name = c.nameResolver.getName(classProto.inlineClassUnderlyingPropertyName)

        // The compiler writes the type into the class itself only when the underlying property is not a part of the ABI
        val typeProto = classProto.inlineClassUnderlyingType(c.typeTable) ?: findInlineClassUnderlyingPropertyTypeProto(name)
        val type = typeProto?.let(::createValueClassUnderlyingTypeBean) ?: return null
        return KotlinInlineClassRepresentation(name, type)
    }

    private fun findInlineClassUnderlyingPropertyTypeProto(name: Name): ProtoBuf.Type? {
        val property = classProto.propertyList.singleOrNull { property ->
            c.nameResolver.getName(property.name) == name &&
                    !property.hasReceiver() &&
                    property.contextParameterList.isEmpty() &&
                    // Fallback for old metadata where context parameters don't exist (KT-74546)
                    property.contextReceiverTypes(c.typeTable).isEmpty()
        }

        return property?.returnType(c.typeTable)
    }

    @OptIn(KtImplementationDetail::class)
    private fun createFullValueClassRepresentation(): KotlinValueClassRepresentation? {
        // An abstract or a sealed value class is not allowed to declare underlying properties, which the compiler denotes with 'null'
        if (isAbstractOrSealed()) return KotlinFullValueClassRepresentation(underlyingPropertyNamesToTypes = null)

        // A full value class stores nothing about its underlying properties, so they are taken from the primary constructor's parameters
        val primaryConstructorProto = primaryConstructorProto ?: return null
        val properties = primaryConstructorProto.valueParameterList.map { parameterProto ->
            val type = createValueClassUnderlyingTypeBean(parameterProto.type(c.typeTable)) ?: return null
            c.nameResolver.getName(parameterProto.name) to type
        }

        return KotlinFullValueClassRepresentation(underlyingPropertyNamesToTypes = properties)
    }

    private fun hasJvmInlineAnnotation(): Boolean = classProto.annotationList.any { annotationProto ->
        c.nameResolver.getClassId(annotationProto.id) == JvmStandardClassIds.Annotations.JvmInline
    }

    private fun createValueClassUnderlyingTypeBean(typeProto: ProtoBuf.Type): KotlinRigidTypeBean? =
        typeStubBuilder.createKotlinTypeBean(typeProto) as? KotlinRigidTypeBean

    private fun computeFoldedProperties(): Map<Name, ProtoBuf.Property> {
        if (classKind != ProtoBuf.Class.Kind.ANNOTATION_CLASS) return emptyMap()

        val parameterNames = primaryConstructorProto?.valueParameterList?.mapTo(mutableSetOf()) {
            c.nameResolver.getName(it.name)
        } ?: return emptyMap()

        return buildMap {
            for (propertyProto in classProto.propertyList) {
                val name = c.nameResolver.getName(propertyProto.name)
                if (name in parameterNames) {
                    put(name, propertyProto)
                }
            }
        }
    }

    private fun createConstructorStub() {
        if (!isClass()) return

        val primaryConstructorProto = primaryConstructorProto ?: return

        createConstructorStub(classOrObjectStub, primaryConstructorProto, c, thisAsProtoContainer, foldedProperties)
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
            ProgressManager.checkCanceled()

            val name = c.nameResolver.getName(entry.name)
            val annotations = c.components.annotationLoader.loadEnumEntryAnnotations(thisAsProtoContainer, entry)
            val enumEntryStub = KotlinEnumEntryStubImpl(
                classBody,
                qualifiedName = c.containerFqName.child(name).ref(),
                name = name.ref(),
                isLocal = false,
            )

            if (annotations.isNotEmpty()) {
                createAnnotationStubs(annotations, createEmptyModifierListStub(enumEntryStub))
            }
        }
    }

    private fun createCallableMemberStubs(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        for (secondaryConstructorProto in classProto.constructorList) {
            ProgressManager.checkCanceled()

            if (Flags.IS_SECONDARY.get(secondaryConstructorProto.flags)) {
                createConstructorStub(classBody, secondaryConstructorProto, c, thisAsProtoContainer, foldedProperties = emptyMap())
            }
        }

        createDeclarationsStubs(classBody, c, thisAsProtoContainer, classProto.functionList, memberPropertyProtos)
    }

    private fun isClass(): Boolean = when (classKind) {
        ProtoBuf.Class.Kind.CLASS, ProtoBuf.Class.Kind.ENUM_CLASS, ProtoBuf.Class.Kind.ANNOTATION_CLASS -> true
        else -> false
    }

    private fun isObject(): Boolean = when (classKind) {
        ProtoBuf.Class.Kind.OBJECT, ProtoBuf.Class.Kind.COMPANION_OBJECT -> true
        else -> false
    }

    private fun isAbstractOrSealed(): Boolean = when (Flags.MODALITY.get(classProto.flags)) {
        ProtoBuf.Modality.ABSTRACT, ProtoBuf.Modality.SEALED -> true
        else -> false
    }

    private fun isInterface(): Boolean = classKind == ProtoBuf.Class.Kind.INTERFACE

    private fun createInnerAndNestedClasses(classBody: KotlinPlaceHolderStubImpl<KtClassBody>) {
        classProto.nestedClassNameList.forEach { id ->
            ProgressManager.checkCanceled()

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
        ProgressManager.checkCanceled()

        (
            val nameResolver, val classProto, val _ = metadataVersion, val sourceElement
        ) =
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
        if (nestedClassId == nameResolver.getClassId(classProto.fqName)) {
            createClassStub(classBody, classProto, nameResolver, nestedClassId, sourceElement, c)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(ClassClsStubBuilder::class.java)
    }
}
