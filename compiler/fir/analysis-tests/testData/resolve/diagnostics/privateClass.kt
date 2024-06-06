// FILE: J1.java

public class J1 extends K2 {}

// FILE: J2.java
public class J2 extends J1 {
    public void foo() {}
}

// FILE: kotlin.kt
private open class K2 : K1() {}

open class K1 {}

class K3 : <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>J2()<!> {}

class K4 : J2 {
    constructor() : <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>super<!>() {}
}

fun foo(): Unit {
    val a = <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>J1()<!>
    val b = K3()
    <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>b.foo()<!>
    val c = <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>J2()<!>
    <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>c.foo()<!>
}
