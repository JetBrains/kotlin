// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75844
// IGNORE_REVERSED_RESOLVE
// FIR_DISABLE_LAZY_RESOLVE_CHECKS

package myPack

import myPack.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>MyObject<!>.*

interface MyInterface<T> {
    val foo: T? get() = null
}

object MyObject : MyInterface<Int>

@Target(<!UNRESOLVED_REFERENCE!>foo<!>)
annotation class MyAnnotation
