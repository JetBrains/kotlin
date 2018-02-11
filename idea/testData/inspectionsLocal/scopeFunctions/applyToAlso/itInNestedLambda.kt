// WITH_RUNTIME

val x = hashSetOf("abc").<caret>apply {
    forEach {
        println(this)
    }
}
