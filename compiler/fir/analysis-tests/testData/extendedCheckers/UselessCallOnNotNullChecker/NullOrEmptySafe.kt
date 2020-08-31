// WITH_RUNTIME

val s: String? = ""
val empty = s?.<!USELESS_CALL_ON_NOT_NULL!>isNullOrEmpty()<!>