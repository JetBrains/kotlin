// FILE: main.kt
fun <T : CharSequence> container(parameter: String, vararg rest: Int): Int {
    lateinit var mutable: String
    val immutable: Int = 0
    val lambda = { value: Int -> value }
    return 0
}

fun String.extension() {}

class WithField {
    var property: Int = 0
}

// FILE: JavaHolder.java
public class JavaHolder {
    public static final String CONSTANT = "";

    public static String mutableText = "";

    protected int counter = 0;
}
