// FILE: p/SAM.java

package p;

public interface SAM<R> {
    R foo();
}

// FILE: p/Util.java

package p;

public class Util {

    public static void sam(SAM<Void> sam) {}
}

// FILE: k.kt

import p.*

fun test() {
    Util.sam {
        null
    }
}