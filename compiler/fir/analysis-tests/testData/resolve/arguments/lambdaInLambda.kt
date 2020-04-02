class StringBuilder {
    fun append(s: String) {}
}

fun buildString(init: StringBuilder.() -> Unit): String {}

interface Template<in X>

class KDocTemplate : Template<StringBuilder> {
    fun definition(content: StringBuilder.() -> Unit) {}
}

fun <U, T : Template<U>> U.insert(template: T, build: T.() -> Unit) {}

fun test(ordinal: Int) {
    buildString {
        insert(KDocTemplate()) {
            definition {
                ordinal?.let {}
            }
        }
    }
}