// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75844
package myPack

import myPack.MyTarget.ANNOTATION_CLASS

enum class MyTarget {
    ANNOTATION_CLASS;
}

@Target(<!ARGUMENT_TYPE_MISMATCH!>ANNOTATION_CLASS<!>)
annotation class MyAnnotation
