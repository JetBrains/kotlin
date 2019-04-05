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

package org.jetbrains.eval4j.test

import junit.framework.TestCase
import junit.framework.TestSuite
import org.jetbrains.eval4j.ExceptionThrown
import org.jetbrains.eval4j.InterpreterResult
import org.jetbrains.eval4j.ObjectValue
import org.jetbrains.eval4j.ValueReturned
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.API_VERSION
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import java.lang.reflect.Array as JArray

fun buildTestSuite(
        create: (MethodNode, Class<*>, InterpreterResult?) -> TestCase
): TestSuite {
    val suite = TestSuite()

    val ownerClass = TestData::class.java
    ownerClass.classLoader!!.getResourceAsStream(ownerClass.getInternalName() + ".class")!!.use { inputStream ->
        ClassReader(inputStream).accept(object : ClassVisitor(API_VERSION) {
            override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                return object : MethodNode(API_VERSION, access, name, desc, signature, exceptions) {
                    override fun visitEnd() {
                        val testCase = buildTestCase(ownerClass, this, create)
                        if (testCase != null) {
                            suite.addTest(testCase)
                        }
                    }
                }
            }
        }, 0)
    }

    return suite
}

private fun buildTestCase(ownerClass: Class<TestData>,
                          methodNode: MethodNode,
                          create: (MethodNode, Class<out Any?>, InterpreterResult?) -> TestCase): TestCase? {
    var expected: InterpreterResult? = null
    for (method in ownerClass.declaredMethods) {
        if (method.name == methodNode.name) {
            val isStatic = (method.modifiers and Modifier.STATIC) != 0
            if (method.parameterTypes!!.size > 0) {
                println("Skipping method with parameters: $method")
            }
            else if (!isStatic && !method.name!!.startsWith("test")) {
                println("Skipping instance method (should be started with 'test') : $method")
            }
            else {
                method.isAccessible = true
                try {
                    val result = method.invoke(if (isStatic) null else ownerClass.newInstance())
                    val returnType = Type.getType(method.returnType!!)
                    expected = ValueReturned(objectToValue(result, returnType))
                }
                catch (e: UnsupportedOperationException) {
                    println("Skipping $method: $e")
                }
                catch (e: Throwable) {
                    val cause = e.cause ?: e
                    expected = ExceptionThrown(objectToValue(cause, Type.getType(cause::class.java)) as ObjectValue, ExceptionThrown.ExceptionKind.FROM_EVALUATOR)
                }
            }
        }
    }

    if (expected == null) {
        println("Method not found: ${methodNode.name}")
        return null
    }

    return create(methodNode, ownerClass, expected)
}