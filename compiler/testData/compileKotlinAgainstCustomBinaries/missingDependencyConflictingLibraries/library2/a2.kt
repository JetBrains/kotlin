package a

class A<T, U: CharSequence, V> {
    inner class Inner<Z>
}

class AA<T, U> {
    inner class Inner<V>
}

class AAA<T> {
    inner class Inner<K> {
        inner class InnerInner<S>
    }
}
