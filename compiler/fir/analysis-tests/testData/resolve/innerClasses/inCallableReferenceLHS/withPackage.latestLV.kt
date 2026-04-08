// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66344, KT-84154
// LATEST_LV_DIFFERENCE

package org.example.foo

class PackageTest<T> {
    inner class A<K> {
        fun foo(){}
        fun usage() {
            org.example.foo.PackageTest<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><*, *><!>.A::foo
        }
    }
}

fun testWithPackage() {
    org.example.foo.PackageTest<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!><String, Int><!>.A::foo
    org.example.foo.<!WRONG_NUMBER_OF_TYPE_ARGUMENTS!>PackageTest<!>.A<String, Int>::foo

    org.example.<!UNRESOLVED_REFERENCE!>foo<!><Int>.PackageTest::class
    org.example.<!UNRESOLVED_REFERENCE!>foo<!><Int, String>.PackageTest.A::class

    org.<!UNRESOLVED_REFERENCE!>example<!><Int, String>.foo.PackageTest.A::class
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, classReference, functionDeclaration, inner, nullableType,
typeParameter */
