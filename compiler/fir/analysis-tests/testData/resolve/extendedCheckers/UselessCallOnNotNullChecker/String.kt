// WITH_STDLIB

val s = ""
val s1 = s.<!USELESS_CALL_ON_NOT_NULL!>orEmpty()<!>