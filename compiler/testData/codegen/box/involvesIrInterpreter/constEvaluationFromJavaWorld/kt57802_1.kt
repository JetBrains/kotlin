// TARGET_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS
// FILE: Bar.java
package one.two;

public class Bar {
    public static final int BAR = Doo.DOO + 1;
}

// FILE: Boo.java
package one.two;

public class Boo {
    public static final int BOO = Baz.BAZ + 1;
}

// FILE: Main.kt
package one.two

class Foo {
    companion object {
        const val FOO = <!EVALUATED("5")!>Boo.BOO + 1<!>
    }
}

class Baz {
    companion object {
        const val BAZ = <!EVALUATED("3")!>Bar.BAR + 1<!>
    }
}

class Doo {
    companion object {
        const val DOO = <!EVALUATED("1")!>1<!>
    }
}

fun box(): String {
    return "OK"
}
