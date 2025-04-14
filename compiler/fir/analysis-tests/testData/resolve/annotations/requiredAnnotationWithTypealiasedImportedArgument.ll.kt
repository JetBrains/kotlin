// LL_FIR_DIVERGENCE
// The compiler crashes with KT-76746
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76746
// IGNORE_REVERSED_RESOLVE

package testPack

import testPack.<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR!>MyTypeAlias<!>.ANNOTATION_CLASS

typealias MyTypeAlias = AnnotationTarget

@Target(ANNOTATION_CLASS)
annotation class MyAnnotation
