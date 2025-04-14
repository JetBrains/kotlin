// IGNORE_FIR
// ISSUE: KT-76746
package testPack

import testPack.MyTypeAlias.ANNOTATION_CLASS

typealias MyTypeAlias = AnnotationTarget

@Target(ANNOTATION_CLASS)
annotation class MyAnn<caret>otation
