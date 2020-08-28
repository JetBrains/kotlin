// FILE: A.java

public class A<T> {
    public T foo(T t) {
        return t;
    }
}

// FILE: simpleFakeOverride.kt


class Some

class B : A<Some>() {
    fun test() {
        foo(Some())
    }
}

