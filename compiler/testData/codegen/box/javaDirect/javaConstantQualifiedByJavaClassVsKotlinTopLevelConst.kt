// TARGET_BACKEND: JVM_IR

// FILE: one/two/Holder.java
package one.two;

public class Holder {
    public static final int VALUE = 2;
}

// FILE: one/two/Bar.java
package one.two;

public class Bar {
    public static final int X = Holder.VALUE;

    public static int viaJava() {
        return X;
    }
}

// FILE: main.kt
package one.two

const val VALUE = 1

const val FROM_KOTLIN = Bar.X

fun box(): String {
    val fromJava = Bar.viaJava()
    return if (FROM_KOTLIN == fromJava) "OK" else "FAIL: kotlin=$FROM_KOTLIN java=$fromJava"
}
