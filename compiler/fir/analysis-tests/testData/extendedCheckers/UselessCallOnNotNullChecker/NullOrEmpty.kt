// WITH_RUNTIME

val s = ""
val empty = s.<!USELESS_CALL_ON_NOT_NULL!>isNullOrEmpty()<!>