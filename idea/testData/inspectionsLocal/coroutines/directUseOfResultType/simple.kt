// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

fun <caret>incorrect() = Result("123")
