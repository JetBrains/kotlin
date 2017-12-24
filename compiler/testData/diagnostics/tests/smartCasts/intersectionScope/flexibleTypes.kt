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
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String?>() }
    }

    if (x is B && x is A) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String?>() }
    }

    if (x is A && x is C) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String?>() }
    }

    if (x is C && x is A) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String?>() }
    }

    if (x is A && x is B && x is C) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String?>() }
    }

    if (x is B && x is A && x is C) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String?>() }
    }

    if (x is B && x is C && x is A) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { _<String>() }
        <!DEBUG_INFO_SMARTCAST!>x<!>.foo().checkType { <!NI;DEBUG_INFO_UNRESOLVED_WITH_TARGET, NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER, OI;TYPE_MISMATCH!>_<!><String?>() }
    }
}
