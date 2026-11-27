// COMPILATION_ERRORS

class Outer<A> {
    inner class Inner<B>
}

fun fo<caret>o(): Outer<String>.Inner<Int, Long>? = null
