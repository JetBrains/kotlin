/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.google.common.annotations.VisibleForTesting
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.sources.JavaSourceElementFactory
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaElement
import org.jetbrains.kotlin.load.java.structure.JavaPackage
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava.Companion.createModuleData
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.deserialization.ErrorReporter
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInsResourceLoader
import java.io.InputStream

/** Creates [JavaClassDescriptor]s of Java classes. */
object JavaClassDescriptorCreator {

    /**
     * Creates [JavaClassDescriptor]s of the given Java classes.
     *
     * Note that creating a [JavaClassDescriptor] for a nested class may require accessing the outer class (and possibly vice versa).
     * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
     *
     * The returned list may contain `null` values for classes whose descriptors can't be created.
     */
    fun create(classesContents: List<ByteArray>): List<JavaClassDescriptor?> {
        val classIds = computeJavaClassIds(classesContents)
        val binaryJavaClasses = classIds.indices.map { index ->
            createBinaryJavaClass(classIds[index], classesContents[index])
        }

        val moduleDescriptor = createModuleData(
            kotlinClassFinder = NoOpKotlinClassFinder,
            jvmBuiltInsKotlinClassFinder = JvmBuiltInsKotlinClassFinder(),
            javaClassFinder = BinaryJavaClassFinder(binaryJavaClasses),
            moduleName = JavaClassDescriptorCreator::class.java.simpleName,
            errorReporter = ThrowImmediatelyErrorReporter,
            javaSourceElementFactory = NoSourceJavaSourceElementFactory
        ).deserializationComponentsForJava.components.moduleDescriptor

        return classIds.map {
            (moduleDescriptor.findClassAcrossModuleDependencies(it) as JavaClassDescriptor?)
                ?: if (it.isLocal || it.isAnonymous()) {
                    // TODO: Handle Java local and anonymous classes later, currently BinaryJavaClass is not able to handle them.
                    null
                } else {
                    error("Failed to create JavaClassDescriptor for class '${it.asString()}'")
                }
        }
    }
}

/**
 * Computes [ClassId]s of the given Java classes.
 *
 * Note that creating a [ClassId] for a nested class will require accessing the outer class to disambiguate any '$' characters in the
 * class name (e.g., "com/example/Foo$Bar" can indicate either a top-level class with simple name "Foo$Bar" or a nested class with simple
 * name "Bar" inside a top-level class with simple name "Foo"). Also note that the outer class can in turn have '$' characters in its name.
 *
 * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
 */
@VisibleForTesting
internal fun computeJavaClassIds(classesContents: List<ByteArray>): List<ClassId> {
    val classInfoList: List<JavaClassNameInfo> = classesContents.map { JavaClassNameInfo.compute(it) }
    val classNameToClassInfo: Map<String, JavaClassNameInfo> = classInfoList.associateBy { it.name }
    val classNameToClassId: MutableMap<String, ClassId?> = classNameToClassInfo.mapValues { null }.toMutableMap()

    fun getOrCreateClassId(className: String): ClassId {
        val classInfo = classNameToClassInfo[className] ?: error("Class name not found: $className")
        val classId = classNameToClassId[className]
        if (classId != null) {
            return classId
        }

        val outerClassId: ClassId? = classInfo.outerName?.let { getOrCreateClassId(it) }

        val packageName = FqName(classInfo.packageName.replace('/', '.'))
        val relativeClassName =
            outerClassId?.let {
                FqName(outerClassId.relativeClassName.asString() + "." + (classInfo.nestedClassSimpleName ?: ""))
            } ?: FqName(classInfo.relativeClassName)
        val isLocal =
            outerClassId?.let {
                // For ClassId, a nested non-local class of a local class is also considered local (see the kdoc of ClassId).
                check(!classInfo.isLocal)
                it.isLocal
            } ?: classInfo.isLocal

        return ClassId(packageName, relativeClassName, isLocal).also {
            classNameToClassId[className] = it
        }
    }

    return classInfoList.map { getOrCreateClassId(it.name) }
}

/** Whether this is an anonymous class (e.g., "com/example/Foo.1"). */
private fun ClassId.isAnonymous(): Boolean {
    val simpleClassName = relativeClassName.asString().substringAfter('.')
    return simpleClassName.isEmpty() || simpleClassName.toIntOrNull() != null
}

private fun createBinaryJavaClass(classId: ClassId, classContents: ByteArray): BinaryJavaClass {
    val context = ClassifierResolutionContext {
        null // TODO: Implement this?
    }
    val outerClass: JavaClass? = null // TODO: Compute outer class?
    val innerClassFinder: ((Name) -> JavaClass?) = { _ ->
        null // TODO: Implement this?
    }

    // Instantiating BinaryJavaClass directly from the Gradle daemon (not the Kotlin daemon) would fail with NoSuchMethodError as one of the
    // parameter types is shaded in kotlin-compiler-embeddable.jar (com.intellij.openapi.vfs.VirtualFile is shaded as
    // org.jetbrains.kotlin.com.intellij.openapi.vfs.VirtualFile), so we have to use reflection here.
    val binaryJavaClass = BinaryJavaClass::class.java
    val constructor = binaryJavaClass.constructors.sortedBy { it.parameters.size }[0]
    return constructor.newInstance(
        /* virtualFile */ null,
        /* fqName */ classId.asSingleFqName(),
        /* context */ context,
        /* signatureParser */ BinaryClassSignatureParser(),
        /* access */ 0,
        /* outerClass */ outerClass,
        /* classContent */ classContents,
        /* innerClassFinder */ innerClassFinder
    ) as BinaryJavaClass
}

/**
 * [JavaClassFinder] that returns results based on the given [BinaryJavaClass] list.
 *
 * Note that some returned results are fake/empty (similar to ReflectJavaClassFinder), but it's probably acceptable for the purpose of this
 * class.
 */
private class BinaryJavaClassFinder(binaryJavaClasses: List<BinaryJavaClass>) : JavaClassFinder {

    private val nameToJavaClass: Map<FqName, BinaryJavaClass> = binaryJavaClasses.associateBy { it.fqName }

    override fun findClass(request: JavaClassFinder.Request): JavaClass? {
        return nameToJavaClass[request.classId.asSingleFqName()]
    }

    override fun findPackage(fqName: FqName): JavaPackage {
        return object : JavaPackage {

            override val fqName: FqName
                get() = fqName

            override val subPackages: Collection<JavaPackage>
                get() = emptyList()

            override val annotations: Collection<JavaAnnotation>
                get() = emptyList()

            override val isDeprecatedInJavaDoc: Boolean
                get() = false

            override fun getClasses(nameFilter: (Name) -> Boolean): Collection<JavaClass> = emptyList()

            override fun findAnnotation(fqName: FqName): JavaAnnotation? = null
        }
    }

    override fun knownClassNamesInPackage(packageFqName: FqName): Set<String>? = null
}

/** [KotlinClassFinder] that returns no results (e.g., when we know that the classes are not Kotlin classes). */
private object NoOpKotlinClassFinder : KotlinClassFinder {
    override fun findKotlinClassOrContent(classId: ClassId): KotlinClassFinder.Result? = null
    override fun findKotlinClassOrContent(javaClass: JavaClass): KotlinClassFinder.Result? = null
    override fun findMetadata(classId: ClassId): InputStream? = null
    override fun hasMetadataPackage(fqName: FqName): Boolean = false
    override fun findBuiltInsData(packageFqName: FqName): InputStream? = null
}

/** [KotlinClassFinder] for Kotlin JVM built-in classes. */
private class JvmBuiltInsKotlinClassFinder : KotlinClassFinder {
    override fun findKotlinClassOrContent(classId: ClassId): KotlinClassFinder.Result? = null
    override fun findKotlinClassOrContent(javaClass: JavaClass): KotlinClassFinder.Result? = null
    override fun findMetadata(classId: ClassId): InputStream? = null
    override fun hasMetadataPackage(fqName: FqName): Boolean = false

    private val builtInsResourceLoader by lazy { BuiltInsResourceLoader() }

    override fun findBuiltInsData(packageFqName: FqName): InputStream? {
        // Same as ReflectKotlinClassFinder.findBuiltInsData
        return if (packageFqName.startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)) {
            builtInsResourceLoader.loadResource(BuiltInSerializerProtocol.getBuiltInsFilePath(packageFqName))
        } else null
    }
}

/** [ErrorReporter] that throws errors as soon as they are reported. */
private object ThrowImmediatelyErrorReporter : ErrorReporter {

    override fun reportIncompleteHierarchy(descriptor: ClassDescriptor, unresolvedSuperClasses: MutableList<String>) {
        // BinaryJavaClassFinder doesn't have the whole classpath so it is unable to resolve classes such as java.lang.Object.
        // Ignore this error for now.
        // FIXME: Revisit later to see how we can address this.
    }

    override fun reportCannotInferVisibility(descriptor: CallableMemberDescriptor) {
        error("Cannot infer visibility for $descriptor")
    }
}

/** [JavaSourceElementFactory] that doesn't provide a source file for a [JavaElement] (e.g., because the source file is not available). */
private object NoSourceJavaSourceElementFactory : JavaSourceElementFactory {

    private class NoSourceJavaElement(override val javaElement: JavaElement) : JavaSourceElement {
        override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
    }

    override fun source(javaElement: JavaElement): JavaSourceElement = NoSourceJavaElement(javaElement)
}

/** Similar to [JavaClassDescriptor.convertToProto] but without an associated source file. */
fun JavaClassDescriptor.toSerializedJavaClass(): SerializedJavaClass {
    val extension = JavaClassesSerializerExtension()

    // Calling DescriptorSerializer.create() directly from the Gradle daemon (not the Kotlin daemon) would fail with NoSuchMethodError as
    // one of the parameter types is shaded in kotlin-compiler-embeddable.jar (com.intellij.openapi.project.Project is shaded as
    // org.jetbrains.kotlin.com.intellij.openapi.project.Project), so we have to use reflection here.
    val createMethod = DescriptorSerializer::class.java.methods.single { it.name == "create" }
    val descriptorSerializer = createMethod.invoke(
        /* static method */ null,
        /* descriptor */ this,
        /* extension */ extension,
        /* parentSerializer */ null,
        /* project */ null
    ) as DescriptorSerializer

    val classProto = descriptorSerializer.classProto(this).build()
    val (stringTable, qualifiedNameTable) = extension.stringTable.buildProto()

    return SerializedJavaClass(classProto, stringTable, qualifiedNameTable)
}
