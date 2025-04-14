// LL_FIR_DIVERGENCE
// KT-76746
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76746
// IGNORE_REVERSED_RESOLVE

package testPack

import testPack.MyTypeAlias.*

typealias MyTypeAlias = AnnotationTarget

@Target(ANNOTATION_CLASS)
annotation class MyAnnotation
