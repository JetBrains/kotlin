// IGNORE_BACKEND: ANDROID
// LANGUAGE: +AllowReturnInExpressionBodyWithExplicitType
// ISSUE: KT-76926

fun box(): String = return "OK"
