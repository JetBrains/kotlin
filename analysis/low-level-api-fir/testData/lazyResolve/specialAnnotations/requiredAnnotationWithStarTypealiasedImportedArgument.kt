// ISSUE: KT-76746

package testPack

import testPack.MyTypeAlias.*

typealias MyTypeAlias = AnnotationTarget

@Target(ANNOTATION_CLASS)
annotation class M<caret>yAnnotation
