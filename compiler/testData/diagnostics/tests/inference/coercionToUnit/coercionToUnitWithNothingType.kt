// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java

public class A {
    public static <T> T flexible(T x) { return x; }
}


// FILE: test.kt

class Inv<K>

fun launch(block: () -> Unit) {}
fun <T> run(block: () -> T): T = block()
fun <T> run(inv: Inv<T>, block: () -> T): T = block()

fun test(i: Inv<Nothing>, iUnit: Inv<Unit>) {
    launch {
        run<Nothing> { TODO("") }
    }
    launch {
        run { TODO("") }
    }
    launch {
        run<String> { "" }
    }
    launch {
        run<Nothing?> { null }
    }
    launch {
        run { null }
    }
    launch {
        run(i) { TODO() }
    }
    launch {
        run(A.flexible(i)) { TODO() }
    }
    launch {
        run(A.flexible(iUnit)) { 42 }
    }
    launch {
        @Suppress("UNSUPPORTED")
        run<dynamic> { "" }
    }

    if (iUnit is <!INCOMPATIBLE_TYPES!>String<!>) {
        launch {
            run(A.flexible(iUnit)) { 42 }
        }
    }
}
