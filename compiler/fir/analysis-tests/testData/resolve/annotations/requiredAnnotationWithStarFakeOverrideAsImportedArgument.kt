// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75844

package myPack

import myPack.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>MyObject<!>.*

interface MyInterface<T> {
    val foo: T? get() = null
}

object MyObject : MyInterface<Int>

@Target(<!ANNOTATION_ARGUMENT_MUST_BE_CONST, ARGUMENT_TYPE_MISMATCH!>foo<!>)
annotation class MyAnnotation
