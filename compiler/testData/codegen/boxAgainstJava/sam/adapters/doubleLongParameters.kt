// FILE: GenericInterface.java

interface GenericInterface<T> {
    public T foo(double d, int i, long j, short s);
}

// FILE: 1.kt

internal fun getInterface(): GenericInterface<String> {
    return GenericInterface { d, i, j, s ->
        "OK"
    }
}

fun box(): String {
    return getInterface().foo(0.0, 0, 0, 0)
}
