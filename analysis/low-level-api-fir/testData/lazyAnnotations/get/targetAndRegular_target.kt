// QUERY: get: kotlin.annotation/Target
// IGNORE_FIR
// WITH_STDLIB
package pack

@AnotherAnno(1) @Target(AnnotationTarget.FIELD)
annotation cla<caret>ss MyAnno

annotation class AnotherAnno(val value: Int)