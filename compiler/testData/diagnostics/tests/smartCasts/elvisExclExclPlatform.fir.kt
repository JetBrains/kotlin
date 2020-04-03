// FILE: p/My.java

package p;

import org.jetbrains.annotations.*;

class My {
    @Nullable static String create() {
        return "";
    }
}

// FILE: test.kt

package p

fun bar(x: String) = x

fun test(x: String?): Any {
    val y = My.create()
    val z = x ?: y!!
    <!INAPPLICABLE_CANDIDATE!>bar<!>(y)
    // !! / ?. is necessary here, because y!! above may not be executed
    y?.hashCode()
    y!!.hashCode()
    return z
}
