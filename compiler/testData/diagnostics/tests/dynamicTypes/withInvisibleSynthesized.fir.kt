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
    fun dynamic.test() {
        <!HIDDEN!>sam<!>(null)
        <!HIDDEN!>sam<!>(
            name = null,
            name = null
        )
    }

    fun test() {
        <!HIDDEN!>sam<!>(null)
    }

}
