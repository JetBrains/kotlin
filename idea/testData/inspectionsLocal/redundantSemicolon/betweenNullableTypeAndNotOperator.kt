// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Boolean?

fun test(a: Any) {
    val b = a as Boolean?;<caret>
    !b
}