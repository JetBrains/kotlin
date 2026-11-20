/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtIdeApi::class, KtImplementationDetail::class)

package org.jetbrains.kotlin.analysis.api.standalone.fir.declarations

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getImportedSimpleNameByImportAlias
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.stubs.KotlinClassOrObjectStub
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.utils.addIfNotNull

/**
 * A declaration in [KotlinStandaloneDeclarationIndex].
 *
 * Its [element] is requested from the [file] by the [ordinal] of the element, so the file may bind its PSI only on the first access.
 */
internal class KotlinStandaloneIndexEntry<out T : PsiElement>(
    private val file: KotlinStandaloneIndexedFile,
    private val ordinal: Int,

    /**
     * The name of a named declaration, available without binding the PSI. `null` for files and scripts.
     */
    val name: Name?,
) {
    val virtualFile: VirtualFile?
        get() = file.virtualFile

    @Suppress("UNCHECKED_CAST")
    val element: T
        get() = file.getElement(ordinal) as T

    override fun equals(other: Any?): Boolean =
        this === other || other is KotlinStandaloneIndexEntry<*> && file === other.file && ordinal == other.ordinal

    override fun hashCode(): Int = 31 * System.identityHashCode(file) + ordinal
}

/**
 * A file in [KotlinStandaloneDeclarationIndex] which provides its indexed elements by their [KotlinStandaloneIndexRecord.ordinal].
 */
internal abstract class KotlinStandaloneIndexedFile(val virtualFile: VirtualFile?) {
    abstract fun getElement(ordinal: Int): PsiElement
}

internal class KotlinStandaloneEagerIndexedFile(
    virtualFile: VirtualFile?,
    private val elements: List<PsiElement>,
) : KotlinStandaloneIndexedFile(virtualFile) {
    override fun getElement(ordinal: Int): PsiElement = elements[ordinal]
}

/**
 * An index entry of a single file, which refers to the indexed element by its [ordinal] instead of PSI.
 *
 * This way, the records of a library file shared between tests are computed once and reused by all projects.
 */
internal sealed class KotlinStandaloneIndexRecord {
    /**
     * The position of the element in the [collectIndexedStubs] list of its file, or in the visiting order for AST-based indexing.
     */
    abstract val ordinal: Int

    class FacadeFile(override val ordinal: Int, val packageFqName: FqName) : KotlinStandaloneIndexRecord()

    class MultifileClassPart(override val ordinal: Int, val facadeFqName: FqName) : KotlinStandaloneIndexRecord()

    class Script(override val ordinal: Int, val fqName: FqName) : KotlinStandaloneIndexRecord()

    class ClassOrObject(
        override val ordinal: Int,
        val classId: ClassId?,
        val name: Name,
        val superNames: List<Name>,
    ) : KotlinStandaloneIndexRecord()

    class TypeAlias(
        override val ordinal: Int,
        val classId: ClassId?,
        val name: Name,
        val inheritableSimpleNames: List<Name>,
    ) : KotlinStandaloneIndexRecord()

    class TopLevelFunction(override val ordinal: Int, val callableId: CallableId) : KotlinStandaloneIndexRecord()

    class TopLevelProperty(override val ordinal: Int, val callableId: CallableId) : KotlinStandaloneIndexRecord()
}

/**
 * The [records] of a file along with the number of its indexed elements.
 *
 * When the records are reused for another project, [indexedElementCount] checks that the elements of the file match their ordinals.
 */
internal class KotlinStandaloneFileIndexRecords(
    val records: List<KotlinStandaloneIndexRecord>,
    val indexedElementCount: Int,
)

/**
 * The stubs of [fileStub] which are indexed by [KotlinStandaloneDeclarationIndex], in a stable order.
 */
internal fun collectIndexedStubs(fileStub: StubElement<*>): List<StubElement<*>> = buildList {
    fun visit(stub: StubElement<*>) {
        when (stub) {
            is KotlinFileStubImpl, is KotlinClassStubImpl, is KotlinObjectStubImpl, is KotlinScriptStubImpl -> {
                add(stub)

                // top-level and member declarations
                stub.childrenStubs.forEach(::visit)
            }

            is KotlinTypeAliasStubImpl, is KotlinFunctionStubImpl, is KotlinPropertyStubImpl -> add(stub)

            is KotlinPlaceHolderStubImpl if (stub.elementType == KtNodeTypes.CLASS_BODY) -> {
                stub.childrenStubs
                    .filter { it is KotlinClassOrObjectStub<*> || it is KotlinTypeAliasStub }
                    .forEach(::visit)
            }

            else -> {}
        }
    }

    visit(fileStub)
}

internal fun collectIndexRecords(indexedStubs: List<StubElement<*>>): List<KotlinStandaloneIndexRecord> = buildList {
    indexedStubs.forEachIndexed { ordinal, stub ->
        addIndexRecords(stub.psi, stub, ordinal)
    }
}

/**
 * Collects the index records of a file without stubs, along with its indexed elements.
 */
internal class KotlinStandaloneAstBasedIndexer : KtVisitorVoid() {
    val elements: MutableList<PsiElement> = mutableListOf()
    val records: MutableList<KotlinStandaloneIndexRecord> = mutableListOf()

    private fun index(element: PsiElement) {
        records.addIndexRecords(element, stub = null, ordinal = elements.size)
        elements += element
    }

    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }

    override fun visitKtFile(file: KtFile) {
        index(file)
        super.visitKtFile(file)
    }

    override fun visitScript(script: KtScript) {
        index(script)
        super.visitScript(script)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        index(classOrObject)
        super.visitClassOrObject(classOrObject)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        index(typeAlias)
        super.visitTypeAlias(typeAlias)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        index(function)
        super.visitNamedFunction(function)
    }

    override fun visitProperty(property: KtProperty) {
        index(property)
        super.visitProperty(property)
    }
}

private fun MutableList<KotlinStandaloneIndexRecord>.addIndexRecords(element: PsiElement, stub: StubElement<*>?, ordinal: Int) {
    when (element) {
        is KtFile -> {
            if (element.hasTopLevelCallables()) {
                add(KotlinStandaloneIndexRecord.FacadeFile(ordinal, element.packageFqName))
            }

            if (stub is KotlinFileStubImpl) {
                val packageFqName = stub.getPackageFqName()
                stub.facadePartSimpleNames?.forEach { partName ->
                    add(KotlinStandaloneIndexRecord.MultifileClassPart(ordinal, packageFqName.child(Name.identifier(partName))))
                }
            }
        }

        is KtScript -> add(KotlinStandaloneIndexRecord.Script(ordinal, element.fqName))

        is KtClassOrObject -> add(
            KotlinStandaloneIndexRecord.ClassOrObject(
                ordinal = ordinal,
                classId = element.getClassId(),
                name = element.nameAsSafeName,
                superNames = element.getSuperNames().map(Name::identifier),
            )
        )

        is KtTypeAlias -> add(
            KotlinStandaloneIndexRecord.TypeAlias(
                ordinal = ordinal,
                classId = element.getClassId(),
                name = element.nameAsSafeName,
                inheritableSimpleNames = element.getTypeReference()?.typeElement
                    ?.let(::findInheritableSimpleNames)
                    .orEmpty()
                    .map(Name::identifier),
            )
        )

        // IDs of nameless declarations use the safe name
        is KtNamedFunction if (element.isTopLevel) ->
            add(KotlinStandaloneIndexRecord.TopLevelFunction(ordinal, element.topLevelCallableId))

        is KtProperty if (element.isTopLevel) ->
            add(KotlinStandaloneIndexRecord.TopLevelProperty(ordinal, element.topLevelCallableId))
    }
}

private val KtCallableDeclaration.topLevelCallableId: CallableId
    get() = CallableId(containingKtFile.packageFqName, nameAsSafeName)

/**
 * This is a simplified version of `KtTypeElement.index()` from the IDE. If we need to move more indexing code to Standalone, we should
 * consider moving more code from the IDE to the Analysis API.
 *
 * @see KotlinStandaloneDeclarationIndex.inheritableTypeAliasesByAliasedName
 */
private fun findInheritableSimpleNames(typeElement: KtTypeElement): List<String> {
    return when (typeElement) {
        is KtUserType -> {
            val referenceName = typeElement.referencedName ?: return emptyList()

            buildList {
                add(referenceName)

                val ktFile = typeElement.containingKtFile
                if (!ktFile.isCompiled) {
                    addIfNotNull(getImportedSimpleNameByImportAlias(typeElement.containingKtFile, referenceName))
                }
            }
        }

        // `typealias T = A?` is inheritable.
        is KtNullableType -> typeElement.innerType?.let(::findInheritableSimpleNames) ?: emptyList()

        else -> emptyList()
    }
}
