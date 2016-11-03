// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: Baz.java

public class Baz {
    public static String baz() {
        return Util.foo() + Util.bar();
    }
}

// FILE: bar.kt

@file:JvmName("Util")
@file:JvmMultifileClass
public fun bar(): String = barx()

public fun foox(): String = "O"

// FILE: foo.kt

@file:JvmName("Util")
@file:JvmMultifileClass
public fun foo(): String = foox()

public fun barx(): String = "K"

// FILE: test.kt

fun box(): String = Baz.baz()
