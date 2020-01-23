// FILE: Utils.java

public class Utils {
    public static E getEnum() {
        return null;
    }
}

// FILE: main.kt

enum class E {
    A, B, C
}

fun test() {
    val e = Utils.getEnum()
    val s = when (e) {
        E.A, null -> return
        E.B -> ""
        E.C -> ""
    }
    s.<!UNRESOLVED_REFERENCE!>length<!>
}
