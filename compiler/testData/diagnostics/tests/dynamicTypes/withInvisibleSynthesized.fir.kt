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
        sam(null)
        sam(
            name = null,
            <!ARGUMENT_PASSED_TWICE!>name<!> = null
        )
    }

    fun test() {
        <!INVISIBLE_REFERENCE!>sam<!>(null)
    }

}
