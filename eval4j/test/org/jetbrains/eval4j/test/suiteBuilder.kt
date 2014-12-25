/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import org.jetbrains.eval4j.*
import org.junit.Assert.*
import junit.framework.TestSuite
import junit.framework.TestCase
import java.lang.reflect.Method
import java.lang.reflect.Field
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Array as JArray
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

fun buildTestSuite(
        create: (MethodNode, Class<*>, InterpreterResult?) -> TestCase
): TestSuite {
    val suite = TestSuite()

    val ownerClass = javaClass<TestData>()
    val inputStream = ownerClass.getClassLoader()!!.getResourceAsStream(ownerClass.getInternalName() + ".class")!!

    ClassReader(inputStream).accept(object : ClassVisitor(ASM5) {

        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return object : MethodNode(ASM5, access, name, desc, signature, exceptions) {
                override fun visitEnd() {
                    val testCase = buildTestCase(ownerClass, this, create)
                    if (testCase != null) {
                        suite.addTest(testCase)
                    }
                }
            }
        }
    }, 0)

    return suite
}

fun buildTestCase(ownerClass: Class<TestData>,
                  methodNode: MethodNode,
                  create: (MethodNode, Class<out Any?>, InterpreterResult?) -> TestCase): TestCase? {
    var expected: InterpreterResult? = null
    for (method in ownerClass.getDeclaredMethods()) {
        if (method.getName() == methodNode.name) {
            val isStatic = (method.getModifiers() and Modifier.STATIC) != 0
            if (method.getParameterTypes()!!.size > 0) {
                println("Skipping method with parameters: $method")
            }
            else if (!isStatic && !method.getName()!!.startsWith("test")) {
                println("Skipping instance method (should be started with 'test') : $method")
            }
            else {
                method.setAccessible(true)
                try {
                    val result = method.invoke(if (isStatic) null else ownerClass.newInstance())
                    val returnType = Type.getType(method.getReturnType()!!)
                    expected = ValueReturned(objectToValue(result, returnType))
                }
                catch (e: UnsupportedOperationException) {
                    println("Skipping $method: $e")
                }
                catch (e: Throwable) {
                    val cause = e.getCause() ?: e
                    expected = ExceptionThrown(objectToValue(cause, Type.getType(cause.javaClass)) as ObjectValue, ExceptionThrown.ExceptionKind.FROM_EVALUATOR)
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