// LANGUAGE: +FullValueClasses

sealed value class Result

value object Empty : Result()

fun test(): Result = <expr>Empty</expr>
