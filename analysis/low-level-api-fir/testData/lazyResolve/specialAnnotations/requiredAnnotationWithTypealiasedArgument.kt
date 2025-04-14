// ISSUE: KT-76746

package testPack

typealias MyTypeAlias = AnnotationTarget

@Target(MyTypeAlias.ANNOTATION_CLASS)
annotation class MyAnn<caret>otation
