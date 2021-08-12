/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

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
     * Note that creating a [JavaClassDescriptor] for a nested class will require accessing the outer class (and possibly vice versa).
     * Therefore, outer classes and nested classes must be passed together in one invocation of this method.
     */
    fun create(classIds: List<ClassId>, classesContents: List<ByteArray>): List<JavaClassDescriptor> {
        val binaryJavaClasses = classIds.mapIndexed { index, classId ->
            createBinaryJavaClass(classId, classesContents[index])
        }
        val moduleDescriptor = createModuleData(
            kotlinClassFinder = NoOpKotlinClassFinder,
            jvmBuiltInsKotlinClassFinder = JvmBuiltInsKotlinClassFinder(),
            javaClassFinder = BinaryJavaClassFinder(binaryJavaClasses),
            moduleName = JavaClassDescriptorCreator::class.java.simpleName,
            errorReporter = ThrowImmediatelyErrorReporter,
            javaSourceElementFactory = NoSourceJavaSourceElementFactory
        ).deserializationComponentsForJava.components.moduleDescriptor

        return classIds.map { classId ->
            moduleDescriptor.findClassAcrossModuleDependencies(classId) as? JavaClassDescriptor
                ?: error("Failed to create JavaClassDescriptor for class '$classId'")
        }
    }
}

private fun createBinaryJavaClass(classId: ClassId, classContents: ByteArray): BinaryJavaClass {
    val context = ClassifierResolutionContext {
        null // TODO: Implement this?
    }
    val outerClass: JavaClass? = null // TODO: Compute outer class?
    val innerClassFinder: ((Name) -> JavaClass?) = { _ ->
        null // TODO: Implement this?
    }

    return try {
        BinaryJavaClass(
            virtualFile = null,
            fqName = classId.asSingleFqName(),
            context = context,
            signatureParser = BinaryClassSignatureParser(),
            access = 0,
            outerClass = outerClass,
            classContent = classContents,
            innerClassFinder = innerClassFinder
        )
    } catch (e: NoSuchMethodError) {
        // When running unit tests, the above call currently fails as JavaClassDescriptorCreator and BinaryJavaClass are located in
        // different jars (kotlin-build-common.jar and kotlin-compiler-embeddable.jar), and in the second jar, `com.intellij.*` classes are
        // repackaged as `org.jetbrains.kotlin.com.intellij.*`. The `virtualFile` argument above (even though it is null) has type
        // `com.intellij.openapi.vfs.VirtualFile`, which has been repackaged, so the method signatures won't match at run time.
        // In integrations tests or regular builds, JavaClassDescriptorCreator and BinaryJavaClass are both located in
        // kotlin-compiler-embeddable.jar with all references renamed together, so there are no issues.
        // We use reflection here as a workaround for unit tests.
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
}

/**
 * [JavaClassFinder] that returns results based on the given [BinaryJavaClass] list.
 *
 * Note that some returned results are fake/empty (similar to ReflectJavaClassFinder).
 * TODO: Revise and see if there are any correctness issues.
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
        // TODO: Revisit later to see how we can address this or if we can safely ignore this error.
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

    val descriptorSerializer = try {
        DescriptorSerializer.create(
            descriptor = this,
            extension = extension,
            parentSerializer = null,
            project = null
        )
    } catch (e: NoSuchMethodError) {
        // When running unit tests, the above call currently fails as the `project` argument above has type
        // `com.intellij.openapi.project.Project`, which is repackaged as `org.jetbrains.kotlin.com.intellij.openapi.project.Project` in
        // kotlin-compiler-embeddable.jar (see the comment at the createBinaryJavaClass method for more context).
        // We use reflection here as a workaround for unit tests.
        val createMethod = DescriptorSerializer::class.java.methods.single { it.name == "create" }
        createMethod.invoke(
            /* static method */ null,
            /* descriptor */ this,
            /* extension */ extension,
            /* parentSerializer */ null,
            /* project */ null
        ) as DescriptorSerializer
    }

    val classProto = descriptorSerializer.classProto(this).build()
    val (stringTable, qualifiedNameTable) = extension.stringTable.buildProto()

    return SerializedJavaClass(classProto, stringTable, qualifiedNameTable)
}
