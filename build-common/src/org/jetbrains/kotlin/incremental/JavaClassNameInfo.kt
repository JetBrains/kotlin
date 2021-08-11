/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.openapi.util.Ref
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Information about the name of a Java class.
 *
 * The hierarchy of the types of classes is as follows:
 *   - Top-level class
 *   - Nested class (https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html)
 *       - Nested non-local class
 *       - Local class (https://docs.oracle.com/javase/tutorial/java/javaOO/localclasses.html)
 */
data class JavaClassNameInfo(

    /** The (full) name of this class (e.g., "com/example/Foo$Bar"). */
    val name: String,

    /** Whether this class is a top-level class (not a nested class). */
    val isTopLevel: Boolean,

    /**
     * The simple name of this class if it is a nested class, or `null` if it is a top-level class (e.g., the simple name of nested class
     * "com/example/Foo$Bar" is "Bar", that of top-level class "com/example/Foo$Bar" is `null`).
     *
     * Note: If this class is an anonymous class, the value may be `null`, empty, or a number (e.g., "1").
     */
    val nestedClassSimpleName: String?,

    /**
     * The (full) name of the outer class of this class if this class is a nested non-local class, or `null` otherwise (e.g., the outer name
     * of nested class "com/example/Foo$Bar" is "com/example/Foo", that of top-level class "com/example/Foo$Bar" is `null`).
     *
     * Note: A local class has an enclosing class (and possibly an enclosing method) but outer name is `null` (see
     * https://asm.ow2.io/javadoc/org/objectweb/asm/ClassVisitor.html#visitOuterClass(java.lang.String,java.lang.String,java.lang.String) and
     * https://asm.ow2.io/javadoc/org/objectweb/asm/ClassVisitor.html#visitInnerClass(java.lang.String,java.lang.String,java.lang.String,int)).
     */
    @Suppress("GrazieInspection")
    val outerName: String?
) {

    /** The package name of this class (e.g., the package name of top-level or nested class "com/example/Foo$Bar" is "com/example"). */
    val packageName: String
        get() = name.substringBeforeLast('/', "")

    /**
     * The name of this class relative to the package (e.g., the relative class name of top-level or nested class "com/example/Foo$Bar" is
     * "Foo$Bar").
     */
    val relativeClassName: String
        get() = name.substringAfterLast('/')

    /**
     * Whether this class is a local class.
     *
     * Note that for `org.jetbrains.kotlin.name.ClassId`, a nested non-local class of a local class is also considered local (see the kdoc
     * of `org.jetbrains.kotlin.name.ClassId`). For [JavaClassNameInfo], a nested non-local class of a local class is still considered
     * non-local.
     */
    val isLocal: Boolean
        get() = !isTopLevel && outerName == null

    companion object {

        fun compute(classContents: ByteArray): JavaClassNameInfo {
            val classNameRef = Ref.create<String>()
            val isTopLevelClassRef = Ref.create<Boolean>()
            val outerClassNameRef = Ref.create<String>()
            val innerClassSimpleNameRef = Ref.create<String>()

            ClassReader(classContents).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visit(
                    version: Int, access: Int, name: String,
                    signature: String?, superName: String?, interfaces: Array<String?>?
                ) {
                    classNameRef.set(name)
                }

                override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
                    if (name == classNameRef.get()!!) {
                        isTopLevelClassRef.set(false)
                        outerClassNameRef.set(outerName)
                        innerClassSimpleNameRef.set(innerName)
                    }
                }
            }, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)

            return JavaClassNameInfo(
                classNameRef.get()!!,
                isTopLevelClassRef.get() ?: true,
                innerClassSimpleNameRef.get(),
                outerClassNameRef.get()
            )
        }
    }
}
