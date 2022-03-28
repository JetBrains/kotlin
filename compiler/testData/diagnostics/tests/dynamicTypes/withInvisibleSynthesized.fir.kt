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
        sam(null)
        sam(
            name = null,
            name = null
        )
    }

    fun test() {
        <!INVISIBLE_REFERENCE!>sam<!>(null)
    }

}
