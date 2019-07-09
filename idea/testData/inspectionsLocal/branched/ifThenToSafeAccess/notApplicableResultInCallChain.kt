// PROBLEM: none
// WITH_RUNTIME
// DISABLE-ERRORS

val someNullableString: String? = ""
fun String.bar(): Result<String> = Result.success("")
val result = if<caret> (someNullableString == null) {
    null
} else {
    someNullableString.bar().getOrNull().let { }
}