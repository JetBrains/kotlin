// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: JavaClass.java
import kotlin.jvm.functions.Function1;

public class JavaClass {
    public <T, R> void foo(Function1<T, R> a){}
}

// FILE: test.kt
val Int.foo: String
    get() = ""

val <T> T.bar: T
    get() = 1 as T

var Int.baz: String
    get() = ""
    set(value) {}

var <T> T.qux: T
    get() = 1 as T
    set(value) { }

fun box(): String {
    JavaClass().foo(Int::foo)
    JavaClass().foo(Int::bar)
    JavaClass().foo(Int::baz)
    JavaClass().foo(Int::qux)
    return "OK"
}