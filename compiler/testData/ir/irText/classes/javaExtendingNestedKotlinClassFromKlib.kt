// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JKLIB
// JKLIB

// MODULE: libA
// FILE: Outer.kt
package test

open class Outer {
    open class Inner {
        val prop: String = "OK"
        fun foo(): String = "OK"
    }
}

// MODULE: libB(libA)
// FILE: JavaClass.java
package test;

public class JavaClass extends Outer.Inner {
}

// MODULE: main(libB libA)
// FILE: Main.kt
package test

fun test() {
    val j = JavaClass()
    val x = j.prop
}
