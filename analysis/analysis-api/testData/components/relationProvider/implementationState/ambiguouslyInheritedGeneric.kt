interface A<T> {
    fun foo(): T
}

interface B<V> {
    fun foo(): V
}

abstract class <caret>Child : A<String>, B<CharSequence>
