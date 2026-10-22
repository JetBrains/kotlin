// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_EXPRESSION
// IDE_MODE
// LANGUAGE: +HomePackageResolution
// RENDER_DIAGNOSTIC_ARGUMENTS

// FILE: a.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("0; 0; 0")!>package first

open class A

fun A.foo() {}
fun A.bar() {}
fun baz(a: A) { }<!>

// FILE: c.kt

<!HOME_PACKAGE_RESOLUTION_INVESTIGATION("3; 0; 2")!>package other

import first.A
import first.foo
import first.bar

fun check(a: A) {
    a.<!DEBUG_INFO_HPR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>foo<!>()
    a.<!DEBUG_INFO_HPR_MIGHT_BE_USED_INSTEAD_OF_IMPORT!>bar<!>()
}<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration */
