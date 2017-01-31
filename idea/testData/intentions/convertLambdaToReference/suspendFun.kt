// IS_APPLICABLE: false
// WITH_RUNTIME

suspend fun String.bar() {

}

suspend fun x() {
    listOf("Jack", "Tom").forEach <caret>{ it.bar() }
}
