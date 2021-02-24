package a

class A<T> {
    inner class Inner<X : Number, Y>
}

class AA<T> {
    inner class Inner<U, V>
}

class AAA<T> {
    inner class Inner<K> {
        inner class InnerInner<S>
    }
}
