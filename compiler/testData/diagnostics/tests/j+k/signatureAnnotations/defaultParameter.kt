// ANDROID_ANNOTATIONS
// FILE: A.java

import kotlin.annotations.jvm.internal.*;

class A {
    public void first(@DefaultValue("hello") String value) {
    }

    public void second(@DefaultValue("first") String a, @DefaultValue("second") String b) {
    }

    public void third(@DefaultValue("first") String a, String b) {
    }

    public void fourth(String first, @DefaultValue("second") String second) {
    }

    public void wrong(@DefaultValue("hello") Integer i) {
    }
}


// FILE: test.kt
fun main() {
    val a = A()

    a.first()
    a.first("arg")

    a.second()
    a.second("arg")
    a.second("first", "second")

    a.third("OK"<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.third("first", "second")

    a.fourth(<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.fourth("first")
    a.fourth("first", "second")

    a.wrong(<!NO_VALUE_FOR_PARAMETER!>)<!>
    a.wrong(42)
}

