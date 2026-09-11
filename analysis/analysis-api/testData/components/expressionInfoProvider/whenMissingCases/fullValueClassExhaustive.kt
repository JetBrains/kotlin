// LANGUAGE: +FullValueClasses
package test

sealed value class Result

value class Success(val value: String, val code: Int) : Result()
value object Empty : Result()
class Failure(val message: String) : Result()

fun test(result: Result): String = <caret>when (result) {
    is Success -> result.value
    Empty -> ""
    is Failure -> result.message
}
