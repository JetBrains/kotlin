// SKIP_TXT

// FILE: JavaClass.java
public class JavaClass {
    public final static String X = "";
    public final static String Y = "";
}

// FILE: main.kt

fun nullableStr(): String? = null

fun foo(w: Int): String {
    var x: String? = nullableStr()

    if (x != null) {
        return x
    }

    // Smartcast should work because when's entries' results are non-flexible
    x = nullableStr() ?: when (w) {
        1 -> JavaClass.X
        else -> JavaClass.Y
    }

    return x
}
