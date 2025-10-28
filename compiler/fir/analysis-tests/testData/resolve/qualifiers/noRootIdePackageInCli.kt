// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-81357

// FILE: test.kt
package test

fun testMe() {}

class Test {
    fun foo() {}
}

fun main1(t: _root_ide_package_.test.Test) {
    _root_ide_package_.test.testMe()
    _root_ide_package_.test.Test()

    val x1 = ::<!UNRESOLVED_REFERENCE!>_root_ide_package_<!>.<!UNRESOLVED_REFERENCE!>test<!>.testMe
    val x2 = _root_ide_package_.test.Test::foo
}

// FILE: main.kt

class RootClass {
    fun foo() {}
}

fun myRootTestMe() {}

fun main2(
    t1: _root_ide_package_.test.Test,
    t2: _root_ide_package_.kotlin.collections.List<_root_ide_package_.test.Test>,
    t3: <!UNRESOLVED_REFERENCE!>_root_ide_package_<!>.RootClass,
) {
    _root_ide_package_.test.testMe()
    _root_ide_package_.test.Test()

    val x1 = ::<!UNRESOLVED_REFERENCE!>_root_ide_package_<!>.<!UNRESOLVED_REFERENCE!>test<!>.testMe
    val x2 = _root_ide_package_.test.Test::foo

    _root_ide_package_.myRootTestMe()
    _root_ide_package_.RootClass()

    val x3 = ::<!UNRESOLVED_REFERENCE!>_root_ide_package_<!>.<!UNRESOLVED_REFERENCE!>myRootTestMe<!>
    val x4 = _root_ide_package_.RootClass::foo

    _root_ide_package_.kotlin.Any()

    <!EXPRESSION_EXPECTED_PACKAGE_FOUND!>_root_ide_package_<!> // property access
    <!UNRESOLVED_REFERENCE!>_root_ide_package_<!>()
}

// FILE: importsCheck.kt
package p3

import <!UNRESOLVED_IMPORT!>_root_ide_package_<!>.test.Test
import <!UNRESOLVED_IMPORT!>_root_ide_package_<!>.test.*
import <!UNRESOLVED_IMPORT!>_root_ide_package_<!>.RootClass
import <!UNRESOLVED_IMPORT!>_root_ide_package_<!>.*

import <!UNRESOLVED_IMPORT!>_root_ide_package_<!>.kotlin.Any as NewAny

fun main3(
    t1: <!UNRESOLVED_REFERENCE!>Test<!>,
    t2: <!UNRESOLVED_REFERENCE!>RootClass<!>,
    t3: <!UNRESOLVED_REFERENCE!>NewAny<!>,
) {
    <!UNRESOLVED_REFERENCE!>Test<!>()
    <!UNRESOLVED_REFERENCE!>RootClass<!>()
    <!UNRESOLVED_REFERENCE!>NewAny<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
