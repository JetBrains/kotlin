/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.index

import com.intellij.psi.PsiJavaModule
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.kotlin.ClassFileHandleKotlinClass
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.InputStream

private val BINARY_CLASS_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS)
private val BINARY_CLASS_AND_SIG_EXTENSIONS = JavaFileExtensions(JavaFileExtension.CLASS, JavaFileExtension.SIG)
private val JAVA_SOURCE_EXTENSIONS = JavaFileExtensions(JavaFileExtension.JAVA)
private val KOTLIN_METADATA_EXTENSIONS = JavaFileExtensions(JavaFileExtension.KOTLIN_METADATA)

/**
 * A wrapper over dependencies [index] for finding Kotlin binary classes and metadata files.
 */
class JvmIndexKotlinClassFinder internal constructor(
    private val index: JvmDependenciesIndex,
    private val filter: ClassFileFilter,
    private val enableSearchInCtSym: Boolean,
    private val perfManager: PerformanceManager?,
) : KotlinClassFinder {
    constructor(
        index: JvmDependenciesIndex,
        classpath: JvmClasspath,
        enableSearchInCtSym: Boolean,
        perfManager: PerformanceManager?,
    ) : this(index, JvmClasspathRootFilter(classpath), enableSearchInCtSym, perfManager)

    /** The `.class`/`.sig` file declaring [classId], if the classpath has one. */
    fun findBinaryClassFile(classId: ClassId): BinaryClassFileHandle? {
        val extensions = if (enableSearchInCtSym) BINARY_CLASS_AND_SIG_EXTENSIONS else BINARY_CLASS_EXTENSIONS
        return findClass(classId, extensions)
    }

    /** [findBinaryClassFile], or else the `.java` file declaring [classId] in a Java source root. */
    fun findSourceOrBinaryClassFile(classId: ClassId): BinaryClassFileHandle? =
        findBinaryClassFile(classId) ?: findClass(classId, JAVA_SOURCE_EXTENSIONS)

    override fun findKotlinClassOrContent(classId: ClassId, metadataVersion: MetadataVersion): KotlinClassFinder.Result? {
        val classFile = findBinaryClassFile(classId) ?: return null
        return readKotlinClass(classFile, metadataVersion)
    }

    /** Only a [BinaryJavaClass] has a class file to read; other Java classes yield `null`. */
    override fun findKotlinClassOrContent(javaClass: JavaClass, metadataVersion: MetadataVersion): KotlinClassFinder.Result? {
        if (javaClass !is BinaryJavaClass) return null
        // A nested class read from bytecode already holds its own class file (see `BinaryJavaClass.findInnerClass`).
        return readKotlinClass(javaClass.classFile, metadataVersion)
    }

    private fun readKotlinClass(classFile: BinaryClassFileHandle, metadataVersion: MetadataVersion): KotlinClassFinder.Result? {
        // Only a `.class` file may carry Kotlin metadata: a `.sig` file never does, and `module-info.class` is not a class.
        if (classFile.extension != JavaFileExtension.CLASS.extension) return null
        if (classFile.nameWithoutExtension == PsiJavaModule.MODULE_INFO_CLASS) return null
        return ClassFileHandleKotlinClass.create(classFile, metadataVersion, perfManager)
    }

    override fun findMetadata(classId: ClassId): InputStream? {
        check(!classId.isNestedClass) { "Nested classes are not supported here: $classId" }

        return findClass(classId, KOTLIN_METADATA_EXTENSIONS)?.readBytes()?.inputStream()
    }

    /** Not restricted by the classpath: this lookup covers all indexed roots. */
    override fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String> {
        val result = ObjectOpenHashSet<String>()
        index.traverseFilesInPackage(packageFqName, KOTLIN_METADATA_EXTENSIONS) { file ->
            result.add(file.nameWithoutExtension)
            true
        }
        return result
    }

    /** Not restricted by the classpath: this lookup covers all indexed roots. */
    override fun hasMetadataPackage(fqName: FqName): Boolean {
        var found = false
        index.traverseFilesInPackage(fqName, KOTLIN_METADATA_EXTENSIONS) { _ ->
            found = true
            false
        }
        return found
    }

    /** Not restricted by the classpath: this lookup covers all indexed roots. */
    override fun findBuiltInsData(packageFqName: FqName): InputStream? =
        index.findBuiltInsFile(packageFqName)?.readBytes()?.inputStream()

    private fun findClass(classId: ClassId, extensions: JavaFileExtensions): BinaryClassFileHandle? =
        index.findClassFiles(classId, extensions, filter).firstOrNull()
}
