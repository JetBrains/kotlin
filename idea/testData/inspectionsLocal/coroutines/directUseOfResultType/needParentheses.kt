// FIX: Add '.getOrThrow()' to function result (breaks use-sites!)
package kotlin

@Suppress("RESULT_CLASS_IN_RETURN_TYPE")
fun <caret>incorrect() = Result("123") + Result("456")
