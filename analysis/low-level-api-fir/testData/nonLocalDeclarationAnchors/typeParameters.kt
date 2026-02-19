class Foo<T> {
    fun <F> bar() {
        fun <D> baz() {

        }
    }

    val <D> D.doo: Int get() = 0
}

fun <F> bar() {
    fun <D> baz() {

    }
}

val <D> D.doo: Int get() = 0
