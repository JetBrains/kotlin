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
    fun <!DYNAMIC_RECEIVER_NOT_ALLOWED, UNSUPPORTED!>dynamic<!>.test() {
        <!DEBUG_INFO_DYNAMIC!>sam<!>(null)
        <!DEBUG_INFO_DYNAMIC!>sam<!>(
            name = null,
            <!ARGUMENT_PASSED_TWICE!>name<!> = null
        <!NO_VALUE_FOR_PARAMETER!>)<!>
    }

    fun test() {
        <!INVISIBLE_MEMBER!>sam<!>(null)
    }

}