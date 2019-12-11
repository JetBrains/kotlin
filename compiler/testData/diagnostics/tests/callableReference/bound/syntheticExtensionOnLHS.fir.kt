// FILE: A.java

class A {
    public CharSequence getFoo() { return null; }
}

// FILE: test.kt

fun test() {
    with (A()) {
        foo::toString
    }
}
