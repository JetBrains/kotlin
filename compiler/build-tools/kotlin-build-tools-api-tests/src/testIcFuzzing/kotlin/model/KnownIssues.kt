/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.fuzzing.model

/**
 * The tracked IC bugs the fuzzer would otherwise keep finding instead of new ones.
 * The edits reproducing them are generated only when they are included by the `kotlin.ic.fuzz.includeKnownIssues` system property:
 * `true` includes all of them, otherwise it is a comma-separated list of the names of the properties below.
 */
object KnownIssues {
    private val includedNames = System.getProperty("kotlin.ic.fuzz.includeKnownIssues").orEmpty().split(',').map(String::trim).toSet()

    private fun included(name: String): Boolean = "true" in includedNames || name in includedNames

    /**
     * KT-89522: a changed default value of an inline function is not propagated to its call sites. The value also changes
     * with the class of its type, e.g. when the class becomes abstract. When excluded, inline functions have only defaults of builtin types.
     */
    val inlineDefaultValueChange: Boolean get() = included("inlineDefaultValueChange")

    /** A change visible only in the `$$forInline` template of a suspend inline function is not propagated to its call sites. */
    val suspendInlineForInlineTemplate: Boolean get() = included("suspendInlineForInlineTemplate")

    /**
     * KT-89613, KT-89649: a change of a class owned by an inline function, here an anonymous object capturing a crossinline lambda,
     * is not propagated to its call sites, which have its copy. This includes the SMAP of the class, which changes with the number
     * of lines of its file. When excluded, crossinline lambdas are called directly.
     */
    val inlineFunOwnedClass: Boolean get() = included("inlineFunOwnedClass")

    /**
     * An inline function compiled together with its call site is inlined unoptimized, and optimized when it is read from a class file,
     * so recompiling only some of the files gives a different bytecode, as in KT-19175. This shows in the `$$forInline` templates
     * of suspend inline functions, in the code inlined through another inline function or an inline property, and in an inlined safe cast
     * which always succeeds or an elvis operator calling the inline `TODO()`, also on the result of an inline function.
     * When excluded, inline functions are not suspend, have no `noinline` parameters or local variables, do not call inline functions
     * or read inline properties, have no `run` body, and have no safe casts which always succeed or elvis operators,
     * and the results of inline functions are not passed to elvis operators.
     * The reproducers are in `SeparateCompilationReproducibilityTest` of the `testIcFuzzingReproducers` suite.
     */
    val inlineOfSeparateCompilation: Boolean get() = included("inlineOfSeparateCompilation")

    /**
     * A class compiled together with a supertype from another file and one compiled against its class file may differ in the order
     * of their constant pools and members, e.g. a class implementing an interface with default members. A file with inline functions
     * compiled together with their call sites may differ in the order of its inner classes, and a diagnostic listing declarations
     * lists them in another order when they are read from a class file: `DiagnosticsReproducibilityTest`.
     * When excluded, the class files differing only in the order of their members, of their constant pool entries and of their
     * attributes are equal; the lines of each member still have to be the same, in the same order. The diagnostics differing only
     * in the order of their lines are equal as well.
     */
    val classFileOrder: Boolean get() = included("classFileOrder")

    /**
     * An import of an enum entry with a body, nested in a private class of another file, is resolved to the class of the entry
     * when the entry is read from a class file, so the incremental build reports another error than the rebuild:
     * `DiagnosticsReproducibilityTest.testImportedEnumEntryWithBodyNestedInPrivateClass`.
     * When excluded, the entries with bodies of private enum classes, also nested in private classes, are not imported.
     */
    val importOfPrivateEnumEntryClass: Boolean get() = included("importOfPrivateEnumEntryClass")

    /**
     * Making an annotation class a `@RequiresOptIn` marker recompiles the declarations annotated with it, but not their usages,
     * so a missing opt-in is not reported: `AnnotationChangesTest.testAnnotationClassBecomingOptInMarker`.
     * When excluded, annotation classes do not become markers.
     */
    val optInMarkerChange: Boolean get() = included("optInMarkerChange")

    /**
     * The compiler crashes on a call using a default value of an inline function with `@JvmExposeBoxed`:
     * `ValidProgramCompilationTest.testJvmExposeBoxedInlineFunctionWithDefaultValue`. When excluded, inline functions are not `@JvmExposeBoxed`.
     */
    val jvmExposeBoxedInline: Boolean get() = included("jvmExposeBoxedInline")

    /**
     * The compiler reports a declaration clash for a value class inheriting a non-null `Unit` property from an interface,
     * also when it overrides the property: `ValidProgramCompilationTest.testValueClassInheritingUnitProperty` and
     * `testValueClassOverridingUnitProperty`. When excluded, the edits making a value class inherit such a property are dropped.
     */
    val valueClassUnitProperty: Boolean get() = included("valueClassUnitProperty")

    /**
     * A reference to a const property has a line number only when the property is compiled together with it:
     * `SeparateCompilationReproducibilityTest.testReferenceToConstProperty` and `testReferenceToConstPropertyOfObject`.
     * When excluded, const properties are not referenced.
     */
    val constPropertyReference: Boolean get() = included("constPropertyReference")

    /**
     * A new subclass of a sealed subclass of a sealed class does not recompile the `when` expressions over the outer sealed class,
     * which may stop being exhaustive: `RecompilationTest.testIndirectSealedSubclass`.
     * When excluded, there are no `when` expressions over a sealed class with a sealed subclass.
     */
    val indirectSealedSubclass: Boolean get() = included("indirectSealedSubclass")

    /**
     * An abstract member added to a nested enum class makes it abstract, but does not recompile the classes listing it as an inner class:
     * `RecompilationTest.testEnumClassBecomingAbstract`. When excluded, only top-level enum classes get abstract members.
     */
    val nestedEnumBecomingAbstract: Boolean get() = included("nestedEnumBecomingAbstract")

    /**
     * A nested class changing its kind, e.g. becoming open, does not recompile the interfaces inheriting members with the class
     * in their signatures, which keep listing it as an inner class with the old flags:
     * `RecompilationTest.testNestedClassBecomingOpenInInheritedSignature`.
     * When excluded, nested classes do not change their kinds while an interface inherits from another one.
     */
    val nestedClassKindInInheritedSignature: Boolean get() = included("nestedClassKindInInheritedSignature")

    /**
     * A changed variance of a type parameter does not recompile the classes inheriting members with the type in their signatures,
     * e.g. an interface whose `DefaultImpls` delegate to its superinterface: `RecompilationTest.testDeclarationSiteVariance`.
     * When excluded, the variances do not change.
     */
    val declarationSiteVariance: Boolean get() = included("declarationSiteVariance")

    /**
     * A changed member of a `fun interface` does not recompile its SAM conversions, which keep implementing the old members:
     * `RecompilationTest.testRenamedFunInterfaceMember` and others. When excluded, lambdas are not SAM converted.
     */
    val funInterfaceChange: Boolean get() = included("funInterfaceChange")

    /**
     * `@JvmDefaultWithoutCompatibility` added to a value class makes the calls of its inherited default interface members call
     * its static `-impl` method instead of the one of the boxed class, but does not recompile them:
     * `RecompilationTest.testValueClassBecomingJvmDefaultWithoutCompatibility`.
     * When excluded, value classes do not toggle `@JvmDefaultWithoutCompatibility`.
     */
    val jvmDefaultOfValueClass: Boolean get() = included("jvmDefaultOfValueClass")

    /**
     * A value class becoming a regular class recompiles the code using it before the inline functions using it,
     * so the compiler crashes on not finding the inline functions in their old class files:
     * `RecompilationTest.testValueClassBecomingRegularClass`. When excluded, value classes do not change their kinds and vice versa.
     */
    val valueClassKindChange: Boolean get() = included("valueClassKindChange")

    /**
     * A renamed file, or a class moved to another file, does not recompile the call sites of the inline members of the classes,
     * whose SMAPs keep the old file name: `RecompilationTest.testRenamedFileWithInlineMemberFunction` and
     * `testMovedClassWithInlineMemberFunction`. The same goes for the top-level inline declarations of a renamed file
     * whose facade is named with `@file:JvmName`: `testRenamedFileWithJvmNameAndInlineFunction`.
     * When excluded, such files are not renamed, and classes with inline members are not moved.
     */
    val renamedFileOfInlineMember: Boolean get() = included("renamedFileOfInlineMember")

    /**
     * A write to or a reference to a `@JvmField` property of an object reads the object only when the object is compiled together
     * with it: `SeparateCompilationReproducibilityTest.testWriteToJvmFieldPropertyOfObject` and
     * `testReferenceToJvmFieldPropertyOfObject`. When excluded, the `@JvmField` properties of objects are not written or referenced.
     */
    val jvmFieldOfObject: Boolean get() = included("jvmFieldOfObject")

    /**
     * A reference to a function or a property of a multifile class is owned by its part only when it is compiled together with it:
     * `SeparateCompilationReproducibilityTest.testReferenceToInlineFunctionOfMultifileClass`, `testReferenceToFunctionOfMultifileClass`
     * and `testReferenceToPropertyOfMultifileClass`. When excluded, the functions and properties of multifile classes are not referenced.
     */
    val multifileReference: Boolean get() = included("multifileReference")
}
