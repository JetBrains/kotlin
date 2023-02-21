/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.psi.impl.compiled.ClassFileStubBuilder
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.SpecialJvmAnnotations
import org.jetbrains.kotlin.analysis.decompiler.stub.*
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType

open class KotlinClsStubBuilder : ClsStubBuilder() {
    override fun getStubVersion() = ClassFileStubBuilder.STUB_VERSION + KotlinStubVersions.CLASSFILE_STUB_VERSION

    override fun buildFileStub(content: FileContent): PsiFileStub<*>? {
        val virtualFile = content.file

        if (ClsClassFinder.isKotlinInternalCompiledFile(virtualFile, content.content)) {
            return null
        }

        if (isVersioned(virtualFile)) {
            // Kotlin can't build stubs for versioned class files, because list of versioned inner classes
            // might be incomplete
            return null
        }

        return doBuildFileStub(virtualFile, content.content)
    }

    private fun doBuildFileStub(file: VirtualFile, fileContent: ByteArray): PsiFileStub<KtFile>? {
        val kotlinClass = ClsKotlinBinaryClassCache.getInstance().getKotlinBinaryClass(file, fileContent)
            ?: error("Can't find binary class for Kotlin file: $file")
        val header = kotlinClass.classHeader
        val classId = kotlinClass.classId
        val packageFqName = header.packageName?.let { FqName(it) } ?: classId.packageFqName

        if (!header.metadataVersion.isCompatibleWithCurrentCompilerVersion()) {
            return createIncompatibleAbiVersionFileStub()
        }

        val components = createStubBuilderComponents(file, packageFqName, fileContent, header.metadataVersion)
        if (header.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS) {
            val partFiles = ClsClassFinder.findMultifileClassParts(file, classId, header.multifilePartNames)
            return createMultifileClassStub(header, partFiles, classId.asSingleFqName(), components)
        }

        val annotationData = header.data
        if (annotationData == null) {
            LOG.error("Corrupted kotlin header for file ${file.name}")
            return null
        }
        val strings = header.strings
        if (strings == null) {
            LOG.error("String table not found in file ${file.name}")
            return null
        }
        return when (header.kind) {
            KotlinClassHeader.Kind.CLASS -> {
                if (classId.isLocal) return null
                val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(annotationData, strings)
                if (Flags.VISIBILITY.get(classProto.flags) == ProtoBuf.Visibility.LOCAL) {
                    // Older Kotlin compiler versions didn't put 'INNERCLASS' attributes in some cases (e.g. for cross-inline lambdas),
                    // so 'ClassFileViewProvider.isInnerClass()' pre-check won't find them (EA-105730).
                    // Example: `Timer().schedule(1000) { foo () }`.
                    return null
                }

                val context = components.createContext(nameResolver, packageFqName, TypeTable(classProto.typeTable))
                createTopLevelClassStub(classId, classProto, KotlinJvmBinarySourceElement(kotlinClass), context, header.isScript)
            }
            KotlinClassHeader.Kind.FILE_FACADE -> {
                val (nameResolver, packageProto) = JvmProtoBufUtil.readPackageDataFrom(annotationData, strings)
                val context = components.createContext(nameResolver, packageFqName, TypeTable(packageProto.typeTable))
                val fqName = header.packageName?.let { ClassId(FqName(it), classId.relativeClassName, classId.isLocal).asSingleFqName() }
                    ?: classId.asSingleFqName()
                createFileFacadeStub(packageProto, fqName, context)
            }
            else -> throw IllegalStateException("Should have processed " + file.path + " with header $header")
        }
    }

    private fun createStubBuilderComponents(
        file: VirtualFile,
        packageFqName: FqName,
        fileContent: ByteArray,
        jvmMetadataVersion: JvmMetadataVersion
    ): ClsStubBuilderComponents {
        val classFinder = DirectoryBasedClassFinder(file.parent!!, packageFqName)
        val classDataFinder = DirectoryBasedDataFinder(classFinder, LOG, jvmMetadataVersion)
        val annotationLoader = AnnotationLoaderForClassFileStubBuilder(classFinder, file, fileContent, jvmMetadataVersion)
        return ClsStubBuilderComponents(classDataFinder, annotationLoader, file)
    }

    companion object {
        val LOG = Logger.getInstance(KotlinClsStubBuilder::class.java)

        // Archive separator + META-INF + versions
        private val VERSIONED_PATH_MARKER = "!/META-INF/versions/"

        fun isVersioned(virtualFile: VirtualFile): Boolean {
            return virtualFile.path.contains(VERSIONED_PATH_MARKER)
        }
    }
}

private class AnnotationLoaderForClassFileStubBuilder(
    kotlinClassFinder: KotlinClassFinder,
    private val cachedFile: VirtualFile,
    private val cachedFileContent: ByteArray,
    override val jvmMetadataVersion: JvmMetadataVersion
) : AbstractBinaryClassAnnotationLoader<AnnotationWithArgs, AnnotationsContainerWithConstants<AnnotationWithArgs, ConstantValue<*>>>(
    kotlinClassFinder
) {

    private val storage =
        LockBasedStorageManager.NO_LOCKS.createMemoizedFunction<KotlinJvmBinaryClass, AnnotationsContainerWithConstants<AnnotationWithArgs, ConstantValue<*>>> { kotlinClass ->
            loadAnnotationsAndInitializers(kotlinClass)
        }

    override fun getAnnotationsContainer(binaryClass: KotlinJvmBinaryClass): AnnotationsContainerWithConstants<AnnotationWithArgs, ConstantValue<*>> {
        return storage(binaryClass)
    }

    override fun getCachedFileContent(kotlinClass: KotlinJvmBinaryClass): ByteArray? {
        if ((kotlinClass as? VirtualFileKotlinClass)?.file == cachedFile) {
            return cachedFileContent
        }
        return null
    }

    override fun loadTypeAnnotation(proto: ProtoBuf.Annotation, nameResolver: NameResolver): AnnotationWithArgs =
        AnnotationWithArgs(nameResolver.getClassId(proto.id), emptyMap())


    override fun loadAnnotation(
        annotationClassId: ClassId, source: SourceElement, result: MutableList<AnnotationWithArgs>
    ): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor {
            private val arguments = mutableMapOf<Name, ConstantValue<*>>()
            override fun visitClassLiteral(name: Name?, value: ClassLiteralValue) {
                if (name != null)
                    arguments[name] = KClassValue(value)
            }

            override fun visitEnd() {
                if (!isRepeatableWithImplicitContainer(annotationClassId, arguments)) {
                    result.add(AnnotationWithArgs(annotationClassId, arguments))
                }
            }

            override fun visit(name: Name?, value: Any?) {
                if (name != null) {
                    arguments[name] = createConstant(value)
                }
            }

            override fun visitEnum(name: Name?, enumClassId: ClassId, enumEntryName: Name) {
                if (name != null) {
                    arguments[name] = EnumValue(enumClassId, enumEntryName)
                }
            }

            override fun visitArray(name: Name?): KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor? {
                if (name == null) return null
                return object : KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor {
                    private val elements = mutableListOf<ConstantValue<*>>()

                    override fun visit(value: Any?) {
                        elements.add(createConstant(value))
                    }

                    override fun visitEnum(enumClassId: ClassId, enumEntryName: Name) {
                        elements.add(EnumValue(enumClassId, enumEntryName))
                    }

                    override fun visitClassLiteral(value: ClassLiteralValue) {
                        elements.add(KClassValue(value))
                    }

                    override fun visitAnnotation(classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor {
                        val list = mutableListOf<AnnotationWithArgs>()
                        val visitor = loadAnnotation(classId, source, list)
                        return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                            override fun visitEnd() {
                                visitor.visitEnd()
                                elements.addAll(list.single().args.map { it.value })
                            }
                        }
                    }

                    override fun visitEnd() {
                        arguments[name] = ArrayValue(elements) { it.builtIns.getArrayElementType(it.builtIns.anyType) }
                    }
                }
            }

            override fun visitAnnotation(name: Name?, classId: ClassId): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                if (name == null) return null
                val list = mutableListOf<AnnotationWithArgs>()
                val visitor = loadAnnotation(classId, source, list)
                return object : KotlinJvmBinaryClass.AnnotationArgumentVisitor by visitor {
                    override fun visitEnd() {
                        visitor.visitEnd()
                        arguments[name] = AnnotationValue(object : AnnotationDescriptor {
                            override val type: KotlinType
                                get() = error("Should not be called")
                            override val allValueArguments: Map<Name, ConstantValue<*>>
                                get() = list.single().args
                            override val source: SourceElement
                                get() = source
                        })
                    }
                }
            }
        }
    }

    fun createConstant(value: Any?): ConstantValue<*> {
        return when (value) {
            is Byte -> ByteValue(value)
            is Short -> ShortValue(value)
            is Int -> IntValue(value)
            is Long -> LongValue(value)
            is Char -> CharValue(value)
            is Float -> FloatValue(value)
            is Double -> DoubleValue(value)
            is Boolean -> BooleanValue(value)
            is String -> StringValue(value)
            is ByteArray -> createArrayValue(value.toList(), PrimitiveType.BYTE)
            is ShortArray -> createArrayValue(value.toList(), PrimitiveType.SHORT)
            is IntArray -> createArrayValue(value.toList(), PrimitiveType.INT)
            is LongArray -> createArrayValue(value.toList(), PrimitiveType.LONG)
            is CharArray -> createArrayValue(value.toList(), PrimitiveType.CHAR)
            is FloatArray -> createArrayValue(value.toList(), PrimitiveType.FLOAT)
            is DoubleArray -> createArrayValue(value.toList(), PrimitiveType.DOUBLE)
            is BooleanArray -> createArrayValue(value.toList(), PrimitiveType.BOOLEAN)
            null -> NullValue()
            else -> error("Unknown value $value")
        }
    }

    private fun createArrayValue(value: List<*>, componentType: PrimitiveType): ArrayValue {
        val elements = value.toList().mapNotNull(this::createConstant)
        return ArrayValue(elements) { it.builtIns.getPrimitiveArrayKotlinType(componentType) }
    }

    protected fun isRepeatableWithImplicitContainer(annotationClassId: ClassId, arguments: Map<Name, ConstantValue<*>>): Boolean {
        if (annotationClassId != SpecialJvmAnnotations.JAVA_LANG_ANNOTATION_REPEATABLE) return false

        val containerKClassValue = arguments[JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME] as? KClassValue ?: return false
        val normalClass = containerKClassValue.value as? KClassValue.Value.NormalClass ?: return false
        return isImplicitRepeatableContainer(normalClass.classId)
    }

    private fun loadAnnotationsAndInitializers(kotlinClass: KotlinJvmBinaryClass): AnnotationsContainerWithConstants<AnnotationWithArgs, ConstantValue<*>> {
        val memberAnnotations = HashMap<MemberSignature, MutableList<AnnotationWithArgs>>()
        val propertyConstants = HashMap<MemberSignature, ConstantValue<*>>()
        val annotationParametersDefaultValues = HashMap<MemberSignature, ConstantValue<*>>()

        kotlinClass.visitMembers(object : KotlinJvmBinaryClass.MemberVisitor {
            override fun visitMethod(name: Name, desc: String): KotlinJvmBinaryClass.MethodAnnotationVisitor {
                return AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString(), desc))
            }

            override fun visitField(name: Name, desc: String, initializer: Any?): KotlinJvmBinaryClass.AnnotationVisitor {
                val signature = MemberSignature.fromFieldNameAndDesc(name.asString(), desc)
                return MemberAnnotationVisitor(signature)
            }

            inner class AnnotationVisitorForMethod(signature: MemberSignature) : MemberAnnotationVisitor(signature),
                KotlinJvmBinaryClass.MethodAnnotationVisitor {

                override fun visitParameterAnnotation(
                    index: Int, classId: ClassId, source: SourceElement
                ): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    val paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index)
                    var result = memberAnnotations[paramSignature]
                    if (result == null) {
                        result = ArrayList()
                        memberAnnotations[paramSignature] = result
                    }
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }

                override fun visitAnnotationMemberDefaultValue(): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return null
                }
            }

            open inner class MemberAnnotationVisitor(protected val signature: MemberSignature) : KotlinJvmBinaryClass.AnnotationVisitor {
                private val result = ArrayList<AnnotationWithArgs>()

                override fun visitAnnotation(classId: ClassId, source: SourceElement): KotlinJvmBinaryClass.AnnotationArgumentVisitor? {
                    return loadAnnotationIfNotSpecial(classId, source, result)
                }

                override fun visitEnd() {
                    if (result.isNotEmpty()) {
                        memberAnnotations[signature] = result
                    }
                }
            }
        }, getCachedFileContent(kotlinClass))

        return AnnotationsContainerWithConstants(
            memberAnnotations,
            propertyConstants,
            annotationParametersDefaultValues
        )
    }
}
