// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76746
// IGNORE_REVERSED_RESOLVE

package testPack

import testPack.MyTypeAlias.*

typealias MyTypeAlias = AnnotationTarget

@Target(<!UNRESOLVED_REFERENCE!>ANNOTATION_CLASS<!>)
annotation class MyAnnotation
