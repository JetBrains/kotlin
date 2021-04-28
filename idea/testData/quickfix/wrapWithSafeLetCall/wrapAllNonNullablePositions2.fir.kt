// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

fun test(s: String?) {
    val s2 = notNull(notNull(<caret>s))
}

fun notNull(name: String): String = name