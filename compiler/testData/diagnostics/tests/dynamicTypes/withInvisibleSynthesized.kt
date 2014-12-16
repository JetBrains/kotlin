// !MARK_DYNAMIC_CALLS

// FILE: p/J.java

package p;

public class J {
    public static class C {
        private void sam(Sam sam) {}
    }


    public interface Sam {
        void sam();
    }
}

// FILE: k.kt

import p.*

class K: J.C() {
    fun <!UNSUPPORTED!>dynamic<!>.test() {
        <!DEBUG_INFO_DYNAMIC!>sam<!>(null)
        <!INVISIBLE_MEMBER!>sam<!>(
            <!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>name<!> = null,
            <!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>name<!> = null
        <!NO_VALUE_FOR_PARAMETER!>)<!>
    }

    fun test() {
        <!INVISIBLE_MEMBER!>sam<!>(null)
    }

}