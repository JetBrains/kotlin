// !WITH_NEW_INFERENCE
// !CHECK_TYPE
// FILE: A.java
public interface A {
    String foo();
}

// FILE: main.kt

interface B {
    fun foo(): String?
}

interface C {
    fun foo(): String
}

fun foo(x: Any?) {
    if (x is A && x is B) {
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        x.foo().checkType { _<String?>() }
    }

    if (x is B && x is A) {
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String>() }
        x.foo().checkType { _<String?>() }
    }

    if (x is A && x is C) {
        x.foo().checkType { _<String>() }
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String?>() }
    }

    if (x is C && x is A) {
        x.foo().checkType { _<String>() }
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String?>() }
    }

    if (x is A && x is B && x is C) {
        x.foo().checkType { _<String>() }
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String?>() }
    }

    if (x is B && x is A && x is C) {
        x.foo().checkType { _<String>() }
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String?>() }
    }

    if (x is B && x is C && x is A) {
        x.foo().checkType { _<String>() }
        x.foo().checkType { <!INAPPLICABLE_CANDIDATE!>_<!><String?>() }
    }
}
