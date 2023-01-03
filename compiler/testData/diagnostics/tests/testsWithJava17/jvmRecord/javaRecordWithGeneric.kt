// FIR_IDENTICAL

// FILE: MyInterface.java
public interface MyInterface<T> {
    T x();
}

// FILE: MyRecord.java
public record MyRecord<T>(T x) implements MyInterface<T> {}

// FILE: main.kt

fun takeInt(x: Int) {}
fun takeString(s: String) {}
fun takeAny(a: Any) {}

fun test_1(mr: MyRecord<Int>) {
    takeInt(mr.x)
    takeInt(mr.x())
}

fun test_2(mr: MyRecord<String>) {
    takeString(mr.x)
    takeString(mr.x())
}

fun test_3(mr: MyRecord<*>) {
    takeAny(mr.x)
    takeAny(mr.x())
}
