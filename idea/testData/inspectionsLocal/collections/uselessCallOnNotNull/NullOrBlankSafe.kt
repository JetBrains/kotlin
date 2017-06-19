// WITH_RUNTIME
// FIX: Rename useless call to 'isBlank'

val s: String? = ""
val blank = s<caret>?.isNullOrBlank()