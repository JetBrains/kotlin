// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: A.java
public interface A<T> {
}

// FILE: B.java
public class B implements A<String> {
}

// FILE: 1.kt
class C: B()

class D: B(), A<String>
class E: B(), A<String?>

fun eatAString(a: A<String>) {}
fun eatAStringN(a: A<String?>) {}

fun test(b: B, c: C, d: D, e: E) {
    eatAString(b)
    eatAString(c)
    eatAString(d)
    eatAString(e)

    eatAStringN(b)
    eatAStringN(c)
    eatAStringN(d)
    eatAStringN(e)
}

// FILE: 3.kt

interface X : A<String>
interface Y: X
interface Z: X

class W: B(), Z

fun test2(w: W) {
    eatAString(w)
    eatAStringN(<!ARGUMENT_TYPE_MISMATCH!>w<!>)
}