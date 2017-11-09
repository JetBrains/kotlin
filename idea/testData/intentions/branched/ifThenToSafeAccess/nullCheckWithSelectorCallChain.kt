// WITH_RUNTIME

val nullableString: String? = "abc"

val foo = if (<caret>nullableString != null) {
    nullableString.toUpperCase().toLowerCase()
} else {
    null
}