// "Wrap with '?.let { ... }' call" "true"
// WITH_RUNTIME

fun test(s: String?) {
    nullable(nullable(notNull(notNull(<caret>s))))
}

fun notNull(name: String): String = name
fun nullable(name: String?): String = ""