package org.jetbrains.eval4j.test

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodNode
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
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.tree.analysis.Frame

fun buildTestSuite(
        create: (MethodNode, Class<*>, InterpreterResult?) -> TestCase
): TestSuite {
    val suite = TestSuite()

    val ownerClass = javaClass<TestData>()
    val inputStream = ownerClass.getClassLoader()!!.getResourceAsStream(ownerClass.getInternalName() + ".class")!!

    ClassReader(inputStream).accept(object : ClassVisitor(ASM4) {

        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return object : MethodNode(access, name, desc, signature, exceptions) {
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
            if ((method.getModifiers() and Modifier.STATIC) == 0) {
                println("Skipping instance method: $method")
            }
            else if (method.getParameterTypes()!!.size > 0) {
                println("Skipping method with parameters: $method")
            }
            else {
                method.setAccessible(true)
                val result = method.invoke(null)
                val returnType = Type.getType(method.getReturnType()!!)
                try {
                    expected = ValueReturned(objectToValue(result, returnType))
                }
                catch (e: UnsupportedOperationException) {
                    println("Skipping $method: $e")
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