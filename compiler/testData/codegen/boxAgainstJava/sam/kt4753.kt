// FILE: Base.java

interface Base<T> {
    void call(T t);
}

// FILE: Derived.java

interface Derived extends Base<String> {
}

// FILE: 1.kt

fun box(): String {
    Base<String>{}.call("")
    Derived{}.call("")
    return "OK"
}
