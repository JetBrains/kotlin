/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.build.report.DoNothingBuildReporter
import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.incremental.classpathDiff.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.asFile
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.ClassFileUtil.snapshot
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotTestCommon.CompileUtil.compileAll
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.sam.SAM_LOOKUP_NAME
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.fail

private val testDataDir =
    File("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/classpathDiff/ClasspathChangesComputerTest")

abstract class ClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    @Test
    abstract fun testAbiVersusNonAbiChanges()

    @Test
    abstract fun testModifiedAddedRemovedElements()

    @Test
    abstract fun testModifiedAddedRemovedElements_ClassLevelSnapshot()

    @Test
    abstract fun testMixedClassSnapshotGranularities()

    @Test
    abstract fun testImpactComputation_SupertypesInheritors()
}

class KotlinOnlyClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiVersusNonAbiChanges() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testAbiVersusNonAbiChanges/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicPropertyChangedType", scope = "com.example.SomeClass"),
                LookupSymbol(name = "publicFunctionChangedSignature", scope = "com.example.SomeClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass"),
            ),
            fqNames = setOf("com.example.SomeClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testModifiedAddedRemovedElements() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testModifiedAddedRemovedElements/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = "modifiedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedProperty", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedFunction", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass",
            )
        ).assertEquals(changes)
    }

    @Test
    override fun testModifiedAddedRemovedElements_ClassLevelSnapshot() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testModifiedAddedRemovedElements/src"), tmpDir, CLASS_LEVEL)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),
                LookupSymbol(name = "ModifiedClassChangedMembers", scope = "com.example"),
                LookupSymbol(name = "AddedClass", scope = "com.example"),
                LookupSymbol(name = "RemovedClass", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass",
            )
        ).assertEquals(changes)
    }

    @Test
    override fun testMixedClassSnapshotGranularities() {
        val currentClasspathSnapshot = testMixedClassSnapshotGranularities_snapshotClasspath("KotlinOnly", "current-classpath", tmpDir)
        val previousClasspathSnapshot = testMixedClassSnapshotGranularities_snapshotClasspath("KotlinOnly", "previous-classpath", tmpDir)

        val changes = computeClasspathChanges(currentClasspathSnapshot, previousClasspathSnapshot)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "modifiedProperty", scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"),
                LookupSymbol(name = "modifiedFunction", scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class")
            ),
            fqNames = setOf(
                "com.example.CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class",
                "com.example.CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class",
                "com.example.FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class",
                "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"
            )
        ).assertEquals(changes)
    }

    @Test
    fun testTopLevelMembers() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testTopLevelMembers/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "modifiedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelProperty", scope = "com.example"),
                LookupSymbol(name = "modifiedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "addedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "removedTopLevelFunction", scope = "com.example"),
                LookupSymbol(name = "movedTopLevelFunction", scope = "com.example"),
            ),
            fqNames = setOf("com.example")
        ).assertEquals(changes)
    }

    @Test
    fun testDifferentClassKinds() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testDifferentClassKinds/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                // NormalClass
                LookupSymbol(name = "propertyInNormalClass", scope = "com.example.NormalClass"),
                LookupSymbol(name = "functionInNormalClass", scope = "com.example.NormalClass"),

                // NormalClass.CompanionObject
                LookupSymbol(name = "propertyInCompanionObject", scope = "com.example.NormalClass.CompanionObject"),
                LookupSymbol(name = "functionInCompanionObject", scope = "com.example.NormalClass.CompanionObject"),

                // NormalClass.NestedClass
                LookupSymbol(name = "propertyInNestedClass", scope = "com.example.NormalClass.NestedClass"),
                LookupSymbol(name = "functionInNestedClass", scope = "com.example.NormalClass.NestedClass"),

                // FileFacade
                LookupSymbol(name = "propertyInFileFacade", scope = "com.example"),
                LookupSymbol(name = "functionInFileFacade", scope = "com.example"),

                // MultifileClass
                LookupSymbol(name = "propertyInMultifileClass1", scope = "com.example"),
                LookupSymbol(name = "functionInMultifileClass1", scope = "com.example"),
                LookupSymbol(name = "propertyInMultifileClass2", scope = "com.example"),
                LookupSymbol(name = "functionInMultifileClass2", scope = "com.example"),

                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.NormalClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.NormalClass.CompanionObject"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.NormalClass.NestedClass"),
            ),
            fqNames = setOf(
                "com.example.NormalClass",
                "com.example.NormalClass.CompanionObject",
                "com.example.NormalClass.NestedClass",
                "com.example"
            )
        ).assertEquals(changes)
    }

    @Test
    fun testConstantsAndInlineFunctions() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testConstantsAndInlineFunctions/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "constantChangedType", scope = "com.example.SomeClass"),
                LookupSymbol(name = "constantChangedValue", scope = "com.example.SomeClass"),

                LookupSymbol(name = "constantChangedType", scope = "com.example.SomeClass.CompanionObject"),
                LookupSymbol(name = "constantChangedValue", scope = "com.example.SomeClass.CompanionObject"),

                LookupSymbol(name = "inlineFunctionChangedSignature", scope = "com.example.SomeClass"),
                LookupSymbol(name = "inlineFunctionChangedImplementation", scope = "com.example.SomeClass"),
                LookupSymbol(name = "inlineFunctionChangedLineNumber", scope = "com.example.SomeClass"),

                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass.CompanionObject"),
            ),
            fqNames = setOf(
                "com.example.SomeClass",
                "com.example.SomeClass.CompanionObject",
            )
        ).assertEquals(changes)
    }

    @Test
    fun testPropertyAccessors() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testPropertyAccessors/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "property_ChangedType", scope = "com.example.SomeClass"),

                LookupSymbol(name = "inlineProperty_ChangedType", scope = "com.example"),
                LookupSymbol(name = "inlineProperty_ChangedType_BackingField", scope = "com.example"),

                LookupSymbol(name = "inlineProperty_ChangedGetterImpl", scope = "com.example"),
                LookupSymbol(name = "inlineProperty_ChangedSetterImpl", scope = "com.example"),

                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass")
            ),
            fqNames = setOf(
                "com.example",
                "com.example.SomeClass"
            )
        ).assertEquals(changes)
    }

    @Test
    fun testFunctionsAndPropertyAccessorsWithJvmNames() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testFunctionsAndPropertyAccessorsWithJvmNames/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedFunction", scope = "com.example.SomeClass"),
                LookupSymbol(name = "changedPropertyAccessor", scope = "com.example.SomeClass"),

                LookupSymbol(name = "changedInlineFunction", scope = "com.example"),
                LookupSymbol(name = "changedInlinePropertyAccessor", scope = "com.example"),

                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass"),
            ),
            fqNames = setOf(
                "com.example",
                "com.example.SomeClass"
            )
        ).assertEquals(changes)
    }

    /** Regression test for KT-55021. */
    @Test
    fun testRenameFileFacade() {
        // Check that classpath changes computation doesn't fail.
        // Ideally, the returned changes should be empty (renaming a file facade alone shouldn't cause any `LookupSymbol`s to change), but
        // it is currently not the case. However, this is just a small efficiency, not a serious bug.
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testRenameFileFacade/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "someFunction", scope = "com.example"),
                LookupSymbol(name = "someProperty", scope = "com.example"),
            ),
            fqNames = setOf("com.example")
        ).assertEquals(changes)
    }

    /** Regression test for KT-58289.*/
    @Test
    fun testChangedAnnotations() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testChangedAnnotations/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "SomeClassWithChangedAnnotation", scope = "com.example"),
            ),
            fqNames = setOf(
                "com.example.SomeClassWithChangedAnnotation",
            )
        ).assertEquals(changes)
    }

    /** Tests [SupertypesInheritorsImpact]. */
    @Test
    override fun testImpactComputation_SupertypesInheritors() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testImpactComputation_SupertypesInheritors/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubSubClassOfChangedSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassOfChangedSuperClass",
                "com.example.SubSubClassOfChangedSuperClass"
            )
        ).assertEquals(changes)
    }

    /**
     * Tests [ConstantsInCompanionObjectsImpact].
     *
     * Note that this test is slightly different from [testConstantsAndInlineFunctions]: In [testConstantsAndInlineFunctions], the companion
     * object's .class file changes, whereas in this test, the companion object's .class file does not change because we want to test that
     * the companion object is unchanged but *impacted* by the change in the .class file of the companion object's outer class.
     */
    @Test
    fun testImpactComputation_ConstantsInCompanionObjects() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinOnly/testImpactComputation_ConstantsInCompanionObjects/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "constantChangedValue", scope = "com.example.SomeClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass"),
                LookupSymbol(name = "constantChangedValue", scope = "com.example.SomeClass.CompanionObject"),
            ),
            fqNames = setOf(
                "com.example.SomeClass",
                "com.example.SomeClass.CompanionObject"
            )
        ).assertEquals(changes)
    }
}

class JavaOnlyClasspathChangesComputerTest : ClasspathChangesComputerTest() {

    @Test
    override fun testAbiVersusNonAbiChanges() {
        val changes = computeClasspathChanges(File(testDataDir, "JavaOnly/testAbiVersusNonAbiChanges/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "publicFieldChangedType", scope = "com.example.SomeClass"),
                LookupSymbol(name = "publicMethodChangedSignature", scope = "com.example.SomeClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SomeClass")
            ),
            fqNames = setOf("com.example.SomeClass")
        ).assertEquals(changes)
    }

    @Test
    override fun testModifiedAddedRemovedElements() {
        val changes = computeClasspathChanges(File(testDataDir, "JavaOnly/testModifiedAddedRemovedElements/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                // ModifiedClassUnchangedMembers
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),

                // ModifiedClassChangedMembers
                LookupSymbol(name = "modifiedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedField", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "modifiedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "addedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = "removedMethod", scope = "com.example.ModifiedClassChangedMembers"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ModifiedClassChangedMembers"),

                // AddedClass
                LookupSymbol(name = "AddedClass", scope = "com.example"),

                // RemovedClass
                LookupSymbol(name = "RemovedClass", scope = "com.example")
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass"
            )
        ).assertEquals(changes)
    }

    @Test
    override fun testModifiedAddedRemovedElements_ClassLevelSnapshot() {
        val changes = computeClasspathChanges(File(testDataDir, "JavaOnly/testModifiedAddedRemovedElements/src"), tmpDir, CLASS_LEVEL)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "ModifiedClassUnchangedMembers", scope = "com.example"),
                LookupSymbol(name = "ModifiedClassChangedMembers", scope = "com.example"),
                LookupSymbol(name = "AddedClass", scope = "com.example"),
                LookupSymbol(name = "RemovedClass", scope = "com.example")
            ),
            fqNames = setOf(
                "com.example.ModifiedClassUnchangedMembers",
                "com.example.ModifiedClassChangedMembers",
                "com.example.AddedClass",
                "com.example.RemovedClass"
            )
        ).assertEquals(changes)
    }

    @Test
    override fun testMixedClassSnapshotGranularities() {
        val currentClasspathSnapshot = testMixedClassSnapshotGranularities_snapshotClasspath("JavaOnly", "current-classpath", tmpDir)
        val previousClasspathSnapshot = testMixedClassSnapshotGranularities_snapshotClasspath("JavaOnly", "previous-classpath", tmpDir)

        val changes = computeClasspathChanges(currentClasspathSnapshot, previousClasspathSnapshot)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class", scope = "com.example"),
                LookupSymbol(name = "modifiedField", scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"),
                LookupSymbol(name = "modifiedMethod", scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class")
            ),
            fqNames = setOf(
                "com.example.CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class",
                "com.example.CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class",
                "com.example.FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class",
                "com.example.FineGrainedFirstBuild_FineGrainedSecondBuild_Class"
            )
        ).assertEquals(changes)
    }

    @Test
    override fun testImpactComputation_SupertypesInheritors() {
        val changes = computeClasspathChanges(File(testDataDir, "JavaOnly/testImpactComputation_SupertypesInheritors/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedField", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.SubSubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubClassOfChangedSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.SubSubClassOfChangedSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedSuperClass",
                "com.example.SubClassOfChangedSuperClass",
                "com.example.SubSubClassOfChangedSuperClass"
            )
        ).assertEquals(changes)
    }
}

class KotlinAndJavaClasspathChangesComputerTest : ClasspathSnapshotTestCommon() {

    // TODO Add more test cases:
    //   - Java class converted to Kotlin class

    @Test
    fun testImpactComputation_SupertypesInheritors() {
        val changes = computeClasspathChanges(File(testDataDir, "KotlinAndJava/testImpactComputation_SupertypesInheritors/src"), tmpDir)
        Changes(
            lookupSymbols = setOf(
                LookupSymbol(name = "changedProperty", scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.KotlinSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedProperty", scope = "com.example.JavaSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.KotlinSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedFunction", scope = "com.example.JavaSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.KotlinSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.JavaSubClassOfChangedKotlinSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.KotlinSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = "changedField", scope = "com.example.JavaSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.KotlinSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = "changedMethod", scope = "com.example.JavaSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.ChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.KotlinSubClassOfChangedJavaSuperClass"),
                LookupSymbol(name = SAM_LOOKUP_NAME.asString(), scope = "com.example.JavaSubClassOfChangedJavaSuperClass")
            ),
            fqNames = setOf(
                "com.example.ChangedKotlinSuperClass",
                "com.example.KotlinSubClassOfChangedKotlinSuperClass",
                "com.example.JavaSubClassOfChangedKotlinSuperClass",
                "com.example.ChangedJavaSuperClass",
                "com.example.KotlinSubClassOfChangedJavaSuperClass",
                "com.example.JavaSubClassOfChangedJavaSuperClass"
            )
        ).assertEquals(changes)
    }
}

private fun testMixedClassSnapshotGranularities_snapshotClasspath(
    language: String, classpathSourceDirName: String, tmpDir: TemporaryFolder
): ClasspathSnapshot {
    val classes = compileAll(File("$testDataDir/$language/testMixedClassSnapshotGranularities/src/$classpathSourceDirName"), tmpDir)

    fun getGranularity(classFile: ClassFile): ClassSnapshotGranularity {
        val granularity = when (val className = classFile.asFile().nameWithoutExtension) {
            "CoarseGrainedFirstBuild_CoarseGrainedSecondBuild_Class" -> CLASS_LEVEL to CLASS_LEVEL
            "CoarseGrainedFirstBuild_FineGrainedSecondBuild_Class" -> CLASS_LEVEL to CLASS_MEMBER_LEVEL
            "FineGrainedFirstBuild_CoarseGrainedSecondBuild_Class" -> CLASS_MEMBER_LEVEL to CLASS_LEVEL
            "FineGrainedFirstBuild_FineGrainedSecondBuild_Class" -> CLASS_MEMBER_LEVEL to CLASS_MEMBER_LEVEL
            else -> error("Unrecognized class: $className")
        }
        return when (classpathSourceDirName) {
            "previous-classpath" -> granularity.first
            "current-classpath" -> granularity.second
            else -> error("Unrecognized classpathSourceDirName: $classpathSourceDirName")
        }
    }

    return classes.map { it.snapshot(getGranularity(it)) }.toClasspathSnapshot()
}

private fun List<ClassSnapshot>.toClasspathSnapshot(): ClasspathSnapshot {
    val classpathEntrySnapshot = ClasspathEntrySnapshot(associateByTo(LinkedHashMap()) {
        JvmClassName.byClassId((it as AccessibleClassSnapshot).classId).internalName + ".class"
    })
    return ClasspathSnapshot(listOf(classpathEntrySnapshot))
}

private fun computeClasspathChanges(
    classpathSourceDir: File,
    tmpDir: TemporaryFolder,
    granularity: ClassSnapshotGranularity? = null
): Changes {
    val currentSnapshot = snapshotClasspath(File(classpathSourceDir, "current-classpath"), tmpDir, granularity)
    val previousSnapshot = snapshotClasspath(File(classpathSourceDir, "previous-classpath"), tmpDir, granularity)
    return computeClasspathChanges(currentSnapshot, previousSnapshot)
}

private fun computeClasspathChanges(
    currentClasspathSnapshot: ClasspathSnapshot,
    previousClasspathSnapshot: ClasspathSnapshot
): Changes {
    return ClasspathChangesComputer.computeChangedAndImpactedSet(
        currentClasspathSnapshot.removeDuplicateAndInaccessibleClasses(),
        previousClasspathSnapshot.removeDuplicateAndInaccessibleClasses(),
        ClasspathSnapshotBuildReporter(DoNothingBuildReporter)
    ).normalize()
}

/** Adapted version of [ChangesEither.Known] for readability in this test. */
private data class Changes(val lookupSymbols: Set<LookupSymbol>, val fqNames: Set<String>)

private fun ProgramSymbolSet.normalize(): Changes {
    val changes: ChangesEither.Known = toChangesEither()
    return Changes(changes.lookupSymbols.toSet(), changes.fqNames.map { it.asString() }.toSet())
}

private fun Changes.assertEquals(actual: Changes) {
    listOfNotNull(
        compare(expected = this.lookupSymbols, actual = actual.lookupSymbols),
        compare(expected = this.fqNames, actual = actual.fqNames)
    ).also {
        if (it.isNotEmpty()) {
            fail(it.joinToString("\n"))
        }
    }
}

private fun compare(expected: Set<*>, actual: Set<*>): String? {
    return if (expected != actual) {
        "Two sets differ:\n" +
                "Elements in expected set but not in actual set: ${expected - actual}\n" +
                "Elements in actual set but not in expected set: ${actual - expected}"
    } else null
}
