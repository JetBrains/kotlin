// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75844
package myPack

import myPack.<!CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON!>MyTarget<!>.*

object MyTarget {
    const val ANNOTATION_CLASS = 0
}

@Target(<!ARGUMENT_TYPE_MISMATCH!>ANNOTATION_CLASS<!>)
annotation class MyAnnotation
