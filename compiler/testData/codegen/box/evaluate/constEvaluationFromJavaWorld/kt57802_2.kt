// TARGET_BACKEND: JVM

// FILE: one/two/Bar.java
package one.two;

public class Bar {
    public static final int BAR = Foo.FOO + 1;
}

// FILE: one/two/Boo.java
package one.two;

public class Boo {
    public static final int BOO = Foo.BAZ + 1;
}

// FILE: Main.kt
package one.two

class Foo {
    companion object {
        const val FOO = 1

        const val BAZ = Bar.BAR + 1

        const val DOO = Boo.BOO + 1
    }
}

fun box(): String {
    return "OK"
}
