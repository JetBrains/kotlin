/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.JvmClassFileArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.utils.rethrow
import org.jetbrains.org.objectweb.asm.*
import java.lang.reflect.Modifier
import java.util.*

/*
 * Test correctness of written flags in class file
 *
 *  TESTED_OBJECT_KIND - maybe class, function or property
 *  TESTED_OBJECTS - className, [function/property name], [function/property signature]
 *  FLAGS - only flags which must be true (could be skipped if ABSENT is TRUE)
 *  ABSENT - true or false, optional (false by default)
 *
 * There could be specified several tested objects separated by empty line, e.g.:
 * TESTED_OBJECT_KIND: property
 * TESTED_OBJECTS: Test$object, prop
 * ABSENT: TRUE
 *
 * TESTED_OBJECT_KIND: property
 * TESTED_OBJECTS: Test, prop$delegate
 * FLAGS: ACC_STATIC, ACC_FINAL, ACC_PRIVATE
 *
 * TESTED_OBJECT_KIND: function
 * TESTED_OBJECTS: Test, function, (ILjava/lang/String;)[Ljava/lang/Object;
 * FLAGS: ACC_PUBLIC, ACC_SYNTHETIC
 */
class JvmWriteFlagsHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        require(info is JvmClassFileArtifact)
        val classFileFactory = info.classFileFactory
        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val fileText = testDataFile.readText()
        val testedObjects: MutableList<TestedObject> = parseExpectedTestedObject(fileText, assertions)
        for (testedObject in testedObjects) {
            var className: String? = null
            for (outputFile in classFileFactory.asList()) {
                val filePath = outputFile.relativePath
                if (testedObject.isFullContainingClassName && filePath == testedObject.containingClass + ".class" ||
                    !testedObject.isFullContainingClassName && filePath.startsWith(testedObject.containingClass)
                ) {
                    className = filePath
                }
            }

            assertions.assertNotNull(className) { "Couldn't find a class file with name ${testedObject.containingClass}" }

            val outputFile = classFileFactory.get(className!!)
            assertions.assertNotNull(outputFile)

            val cr = ClassReader(outputFile!!.asByteArray())
            var classVisitor: TestClassVisitor = getClassVisitor(testedObject, false)
            cr.accept(classVisitor, ClassReader.SKIP_CODE)

            if (!classVisitor.isExists) {
                classVisitor = getClassVisitor(testedObject, true)
                cr.accept(classVisitor, ClassReader.SKIP_CODE)
            }

            val isObjectExists = !InTextDirectivesUtils.findStringWithPrefixes(testedObject.textData!!, "// ABSENT: ").toBoolean()
            assertions.assertEquals(isObjectExists, classVisitor.isExists) { "Wrong object existence state: $testedObject" }

            if (isObjectExists) {
                val expected: Int = getExpectedFlags(testedObject.textData!!)
                val actual = classVisitor.access
                if (expected != actual) {
                    assertions.assertEquals(flagsToText(expected), flagsToText(actual)) {
                        "Wrong access flag for $testedObject \n${outputFile.asText()}"
                    }
                }
            }
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private class TestedObject {
        var name: String? = null
        var containingClass: String = ""
        var isFullContainingClassName: Boolean = true
        var kind: String? = null
        var textData: String? = null
        var signature: String? = null

        override fun toString(): String {
            return "Class = " + containingClass + ", name = " + name + ", kind = " + kind +
                    (if (signature != null) ", signature = $signature" else "")
        }
    }

    private abstract class TestClassVisitor : ClassVisitor(Opcodes.API_VERSION) {
        var isExists: Boolean = false
            protected set

        abstract val access: Int
    }

    private class ClassFlagsVisitor : TestClassVisitor() {
        override var access = 0

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String?>?) {
            this.access = access
            isExists = true
        }
    }

    private class FunctionFlagsVisitor(
        private val funName: String,
        private val funSignature: String?,
        private val allowSynthetic: Boolean
    ) : TestClassVisitor() {
        override var access = 0

        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String?>?): MethodVisitor? {
            if (name == funName && (funSignature == null || funSignature == desc)) {
                if (!allowSynthetic && (access and Opcodes.ACC_SYNTHETIC) != 0) return null
                this.access = access
                isExists = true
            }
            return null
        }
    }

    private class PropertyFlagsVisitor(private val propertyName: String, private val propertySignature: String?) : TestClassVisitor() {
        override var access = 0

        override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
            if (name == propertyName && (propertySignature == null || propertySignature == desc)) {
                this.access = access
                isExists = true
            }
            return null
        }
    }

    private class InnerClassFlagsVisitor(private val innerClassName: String) : TestClassVisitor() {
        override var access = 0

        override fun visitInnerClass(innerClassInternalName: String, outerClassInternalName: String?, name: String?, access: Int) {
            if (innerClassName == name) {
                this.access = access
                isExists = true
            }
        }
    }

    companion object {
        private fun parseExpectedTestedObject(testDescription: String, assertions: Assertions): MutableList<TestedObject> {
            val testObjectData = testDescription.substring(testDescription.indexOf("// TESTED_OBJECT_KIND")).split("\n\n".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
            val objects: MutableList<TestedObject> = ArrayList<TestedObject>()

            for (testData in testObjectData) {
                if (testData.isEmpty()) continue

                val testObject = TestedObject()
                testObject.textData = testData
                val testedObjects = InTextDirectivesUtils.findListWithPrefixes(testData, "// TESTED_OBJECTS: ")
                assertions.assertFalse(testedObjects.isEmpty()) { "Cannot find TESTED_OBJECTS instruction" }
                testObject.containingClass = testedObjects[0]
                when (testedObjects.size) {
                    1 -> testObject.name = testedObjects[0]
                    2 -> testObject.name = testedObjects[1]
                    3 -> {
                        testObject.name = testedObjects[1]
                        testObject.signature = testedObjects[2]
                    }
                    else -> {
                        throw IllegalArgumentException(
                            "TESTED_OBJECTS instruction must contain one (for class), two or three (for function and property) values"
                        )
                    }
                }

                testObject.kind = InTextDirectivesUtils.findStringWithPrefixes(testData, "// TESTED_OBJECT_KIND: ")
                val isFullName = InTextDirectivesUtils.findListWithPrefixes(testData, "// IS_FULL_CONTAINING_CLASS_NAME: ")
                if (isFullName.size == 1) {
                    testObject.isFullContainingClassName = isFullName[0].toBoolean()
                }
                objects.add(testObject)
            }
            assertions.assertFalse(objects.isEmpty()) { "Test description not present!" }
            return objects
        }

        private fun getClassVisitor(`object`: TestedObject, allowSynthetic: Boolean): TestClassVisitor {
            return when (`object`.kind) {
                "class" -> ClassFlagsVisitor()
                "function" -> FunctionFlagsVisitor(`object`.name!!, `object`.signature, allowSynthetic)
                "property" -> PropertyFlagsVisitor(`object`.name!!, `object`.signature)
                "innerClass" -> InnerClassFlagsVisitor(`object`.name!!)
                else -> throw IllegalArgumentException("Value of TESTED_OBJECT_KIND is incorrect: " + `object`.kind)
            }
        }

        private fun flagsToText(flags: Int): String {
            val sb = StringBuilder()
            sb.append(flags).append(" = ").append(String.format("0x%04x", flags)).append("\n")
            var flag = 1
            while (flag > 0) {
                if ((flags and flag) != 0) {
                    val string: String? = FLAG_TO_STRING[flag]
                    sb.append(string ?: "unknown ($flag)").append("\n")
                }
                flag = flag shl 1
            }
            return sb.toString()
        }

        private fun getExpectedFlags(text: String): Int {
            var expectedAccess = 0
            val flags = InTextDirectivesUtils.findListWithPrefixes(text, "// FLAGS: ")
            for (flag in flags) {
                try {
                    val field = Opcodes::class.java.getDeclaredField(flag)
                    expectedAccess = expectedAccess or field.getInt(null)
                } catch (e: NoSuchFieldException) {
                    throw IllegalArgumentException("Cannot find $flag field in Opcodes class", e)
                } catch (e: IllegalAccessException) {
                    throw IllegalArgumentException("Cannot find $flag field in Opcodes class", e)
                }
            }
            return expectedAccess
        }

        private val FLAG_TO_STRING: Map<Int, String>

        init {
            val flagToString = hashMapOf<Int, String>()
            try {
                for (field in Opcodes::class.java.getDeclaredFields()) {
                    val name = field.name
                    if (Modifier.isStatic(field.modifiers) && name.startsWith("ACC_")) {
                        val value = field.getInt(null)
                        val previous = flagToString[value]
                        flagToString[value] = if (previous == null) name else "$previous/$name"
                    }
                }
            } catch (e: IllegalAccessException) {
                throw rethrow(e)
            }
            FLAG_TO_STRING = Collections.unmodifiableMap(flagToString)
        }
    }
}
