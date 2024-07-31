// QUERY: get: kotlin.annotation/Target
// WITH_STDLIB
package pack

@AnotherAnnotation("str")
annotation cla<caret>ss MyAnno

annotation class AnotherAnnotation(val value: String)