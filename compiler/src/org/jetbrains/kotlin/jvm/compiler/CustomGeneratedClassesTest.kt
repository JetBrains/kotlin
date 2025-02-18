/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler

import com.intellij.testFramework.BinaryLightVirtualFile
import junit.framework.TestCase
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaClass
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.junit.Test

class CustomGeneratedClassesTest : TestCase() {

    @Test
    fun testEmulatedScalaStdlibSyntheticMethodLoading() {
        // #KT-38325 and #KT-39799
        val classFqn = "org/jetbrains/kotlin/compiler/test/GeneratedScalalikeTraversableOncePart"

        val classNode = ClassNode(Opcodes.API_VERSION).apply {
            version = Opcodes.V1_6
            access = Opcodes.ACC_PUBLIC
            name = classFqn
            signature = "L$classFqn;"
            superName = "java/lang/Object"
            methods.add(
                // The root of the problem described in the #KT-38325 and #KT-39799 is the presence of a method with signature and descriptor
                // disagreeing on the number of parameters. Here the method with the similar structure is created
                MethodNode(
                    Opcodes.API_VERSION,
                    Opcodes.ACC_PRIVATE,
                    "reverser\$2",
                    "(Ljava/lang/Object;)L$classFqn\$reverser\$1\$;",
                    "()L$classFqn\$reverser\$1\$;",
                    null
                ).apply {
                    visitParameter("a", 0)
                }
            )
        }

        val classWriter = ClassWriter(0).also {
            classNode.accept(it)
        }

        // This is the actual test. Without the relevant fix, this call throws "No parameter with index 0-0" error
        BinaryJavaClass(
            BinaryLightVirtualFile("$classFqn.class", classWriter.toByteArray()),
            FqName(classFqn.replace('/', '.')),
            ClassifierResolutionContext { null },
            BinaryClassSignatureParser(),
            outerClass = null
        )
    }
}