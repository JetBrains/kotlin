// !LANGUAGE: +InlineClasses -AllowResultInReturnType

fun result(): <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = TODO()
val resultP: <!RESULT_CLASS_IN_RETURN_TYPE!>Result<Int><!> = result()
