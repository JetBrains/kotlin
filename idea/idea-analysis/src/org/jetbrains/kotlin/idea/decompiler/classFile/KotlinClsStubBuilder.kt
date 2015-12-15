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

package org.jetbrains.kotlin.idea.decompiler.classFile

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.*
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.load.kotlin.AbstractBinaryClassAnnotationAndConstantLoader
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleFileFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleMultifileClassKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager

public open class KotlinClsStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + 1

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val file = content.getFile()

        if (isKotlinInternalCompiledFile(file)) {
            return null
        }

        return doBuildFileStub(file)
    }

    fun doBuildFileStub(file: VirtualFile): PsiFileStub<KtFile>? {
        val kotlinBinaryClass = KotlinBinaryClassCache.getKotlinBinaryClass(file)!!
        val header = kotlinBinaryClass.getClassHeader()
        val classId = kotlinBinaryClass.getClassId()
        val packageFqName = classId.getPackageFqName()
        if (!header.isCompatibleAbiVersion) {
            return createIncompatibleAbiVersionFileStub()
        }

        val components = createStubBuilderComponents(file, packageFqName)
        if (header.isCompatibleMultifileClassKind()) {
            val partFiles = findMultifileClassParts(file, kotlinBinaryClass)
            return createMultifileClassStub(kotlinBinaryClass, partFiles, classId.asSingleFqName(), components)
        }

        val annotationData = header.annotationData
        if (annotationData == null) {
            LOG.error("Corrupted kotlin header for file ${file.getName()}")
            return null
        }
        val strings = header.strings
        if (strings == null) {
            LOG.error("String table not found in file ${file.getName()}")
            return null
        }
        return when {
            header.isCompatibleClassKind() -> {
                if (header.isLocalClass) return null
                val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(annotationData, strings)
                val context = components.createContext(nameResolver, packageFqName, TypeTable(classProto.typeTable))
                createTopLevelClassStub(classId, classProto, context)
            }
            header.isCompatibleFileFacadeKind() -> {
                val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(annotationData, strings)
                val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))
                createFileFacadeStub(packageProto, classId.asSingleFqName(), context)
            }
            else -> throw IllegalStateException("Should have processed " + file.getPath() + " with header $header")
        }
    }

    private fun createStubBuilderComponents(file: VirtualFile, packageFqName: FqName): ClsStubBuilderComponents {
        val classFinder = DirectoryBasedClassFinder(file.getParent()!!, packageFqName)
        val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG)
        val annotationLoader = AnnotationLoaderForClassFileStubBuilder(classFinder, LoggingErrorReporter(LOG))
        return ClsStubBuilderComponents(classDataFinder, annotationLoader)
    }

    companion object {
        val LOG = Logger.getInstance(KotlinClsStubBuilder::class.java)
    }
}

class AnnotationLoaderForClassFileStubBuilder(
        kotlinClassFinder: KotlinClassFinder,
        errorReporter: ErrorReporter
) : AbstractBinaryClassAnnotationAndConstantLoader<ClassId, Unit, ClassIdWithTarget>(
        LockBasedStorageManager.NO_LOCKS, kotlinClassFinder, errorReporter) {

    override fun loadClassAnnotations(
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver
    ): List<ClassId> {
        val binaryAnnotationDescriptors = super.loadClassAnnotations(classProto, nameResolver)
        val serializedAnnotations = classProto.getExtension(JvmProtoBuf.classAnnotation).orEmpty()
        val serializedAnnotationDescriptors = serializedAnnotations.map { nameResolver.getClassId(it.id) }
        return binaryAnnotationDescriptors + serializedAnnotationDescriptors
    }

    override fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): ClassId =
            nameResolver.getClassId(proto.getId())

    override fun loadConstant(desc: String, initializer: Any) = null

    override fun loadAnnotation(
            annotationClassId: ClassId, source: SourceElement, result: MutableList<ClassId>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
        result.add(annotationClassId)
        return null
    }

    override fun loadPropertyAnnotations(propertyAnnotations: List<ClassId>, fieldAnnotations: List<ClassId>): List<ClassIdWithTarget> {
        return propertyAnnotations.map { ClassIdWithTarget(it, null) } +
               fieldAnnotations.map { ClassIdWithTarget(it, AnnotationUseSiteTarget.FIELD) }
    }

    override fun transformAnnotations(annotations: List<ClassId>) = annotations.map { ClassIdWithTarget(it, null) }
}
