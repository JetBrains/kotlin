/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * The classpath-wide state of the binary Java model: class-file lookups in a [BinaryClassFileIndex] and the
 * classes read from them. This state is created once per compilation, by whoever builds the Java view of that
 * compilation, and shared by the binary `JavaClassFinder` of every session: the JDK and the libraries are then
 * looked up and parsed once, and a session applies its own [BinaryClassFileScope] after the lookup.
 */
class BinaryJavaClassCache(private val index: BinaryClassFileIndex) {

    val signatureParser: BinaryClassSignatureParser = BinaryClassSignatureParser()

    val classes: BinaryJavaClasses = BinaryJavaClasses()

    // Indexed by the two parts of the outermost class name as they already exist in a `ClassId`. An `FqName`
    // of that class would be a nicer single key, but building it costs a string concatenation, an `FqName`,
    // an `FqNameUnsafe`, a `pathSegments()` list and a hash of a fresh string on every lookup.
    private val topLevelClassFiles: MutableMap<FqName, MutableMap<Name, Collection<BinaryClassFileHandle>>> = HashMap()

    private val classFileNamesInPackage: MutableMap<FqName, Set<String>> = HashMap()

    fun findTopLevelClassFiles(packageFqName: FqName, topLevelName: Name): Collection<BinaryClassFileHandle> =
        topLevelClassFiles.getOrPut(packageFqName) { HashMap() }.getOrPut(topLevelName) {
            index.findTopLevelClassFiles(ClassId(packageFqName, topLevelName))
        }

    fun classFileNamesInPackage(packageFqName: FqName): Set<String> =
        classFileNamesInPackage.getOrPut(packageFqName) { index.classFileNamesInPackage(packageFqName) }

    fun containsPackageDirectory(packageFqName: FqName): Boolean = index.containsPackageDirectory(packageFqName)
}
