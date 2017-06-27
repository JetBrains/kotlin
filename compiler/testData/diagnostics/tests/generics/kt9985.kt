// !CHECK_TYPE
// Incorrect "type mismatch" error for generic extension safe call (required not-null, found nullable)

// FILE: B.java

public class B<T> {
    public String gav() {
        return "";
    }

    public static <T> B<T> create() {
        return new B();
    }
}

// FILE: A.kt

class A<T> {
    fun gav() = ""
}
fun <R> foo(x: R) = x
fun <T> A<T>.bar() = ""
fun <T> B<T>.bar() = ""

fun foo(l: A<String>?) {
    // No errors should be here
    foo(l?.bar()) checkType { _<String?>() }
    foo(l?.gav()) checkType { _<String?>() }
    if (l != null) {
        foo(l<!UNNECESSARY_SAFE_CALL!>?.<!>bar()) checkType { _<String>() }
        foo(l<!UNNECESSARY_SAFE_CALL!>?.<!>gav()) checkType { _<String>() }
    }
}

fun fooNotNull(l: A<String>) {
    // No errors should be here
    foo(l<!UNNECESSARY_SAFE_CALL!>?.<!>bar()) checkType { _<String>() }
    foo(l<!UNNECESSARY_SAFE_CALL!>?.<!>gav()) checkType { _<String>() }
}

fun bar() {
    val l = B.create<String>()
    foo(l?.bar()) checkType { _<String?>() }
    foo(l?.gav()) checkType { _<String?>() }
}

