// ISSUE: KT-91111
// On JVM, the default value is moved to the static `foo$default`, which has the type parameter of `foo` but not the one of `Outer`,
// while the type of the anonymous object still has both of them as arguments.
// The private `bar` is called from another class, so the call goes through a synthetic accessor,
// and `SyntheticAccessorGenerator` builds a type substitutor from the type of the receiver.

class Outer<T> {
    fun <R> foo(
        x: Any = object {
            private fun bar(): String = "OK"

            override fun toString(): String = object {
                fun baz(): String = bar()
            }.baz()
        }
    ): String = x.toString()
}

fun box(): String = Outer<Int>().foo<String>()
