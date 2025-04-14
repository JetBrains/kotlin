// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76746
// IGNORE_REVERSED_RESOLVE

package testPack

import testPack.MyTypeAlias.ANNOTATION_CLASS

typealias MyTypeAlias = AnnotationTarget

@Target(ANNOTATION_CLASS)
annotation class MyAnnotation
