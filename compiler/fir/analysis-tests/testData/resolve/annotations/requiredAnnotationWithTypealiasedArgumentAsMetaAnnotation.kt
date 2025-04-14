// RUN_PIPELINE_TILL: BACKEND
// FIR_DUMP
// IGNORE_REVERSED_RESOLVE
// ^Reverse order has the same problem as CLI mode –
//   it dosn't move the annotation to the backing field as
//   '@Target' arguments are unresolved on the compiler required phase

package testPack

typealias MyTypeAlias = AnnotationTarget

@Target(MyTypeAlias.FIELD)
annotation class MyAnnotation

@MyAnnotation
val property = 0
