// DONT_TARGET_EXACT_BACKEND: JS JS_IR JS_IR_ES6 WASM NATIVE
// MODULE: lib
// FILE: JFoo.java
public interface JFoo<T> {
    T foo(T x);
}

// FILE: J.java
public class J {
    public static void use(JFoo<String> jfoo) {
        jfoo.foo(null);
    }
}


// MODULE: main(lib)
// FILE: test.kt
fun test(a: Any) {
    a as (String) -> String
    J.use(a)
}

fun box(): String {
    test({s: String? -> s})
    return "OK"
}
