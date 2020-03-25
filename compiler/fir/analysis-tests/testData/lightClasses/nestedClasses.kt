interface A<T>

class B<E> {
    inner class C : A<E>
    class D : A<String>
}

// LIGHT_CLASS_FQ_NAME: B
