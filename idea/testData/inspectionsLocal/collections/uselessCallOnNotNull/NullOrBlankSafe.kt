// WITH_RUNTIME
// FIX: Change call to 'isBlank'

val s: String? = ""
val blank = s<caret>?.isNullOrBlank()