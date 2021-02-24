interface A<out T> {
    val value: T
}

interface B<out T : CharSequence> : A<T>

open class C(override val value: String) : B<CharSequence>

interface X {
    val value: CharSequence
}

class Y(value: String) : C(value), X
