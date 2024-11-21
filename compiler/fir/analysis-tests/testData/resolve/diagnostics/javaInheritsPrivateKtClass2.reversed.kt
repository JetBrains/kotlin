// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71508
// ISSUE: KT-71480
// ISSUE: KT-71511
// ISSUE: KT-73387  vvvv
// LANGUAGE: +ProhibitJavaClassInheritingPrivateKotlinClass

// FILE: J1.java
public class J1 extends Outer.Base1 { }

// FILE: J2.java
public class J2 extends Base2 { }

// FILE: J3.java
public class J3 extends Base3 { }

// FILE: kotlin.kt
private class Outer {
    open class Base1 {
        fun foo() {}
    }
}

private open class Base2 {
    fun foo(){}
}

private interface BaseInterface {
    fun foo() = 0 // private implementation is exposed via Base3
}

public open class Base3 : BaseInterface {
}


fun test1(){
    <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!><!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>J1()<!>.foo()<!>
}

fun test2(a: J1){
    <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>a.foo()<!>
    <!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!><!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>J2()<!>.foo()<!>
}

fun test3() {
    <!ERROR_FROM_JAVA_RESOLUTION, JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!><!JAVA_CLASS_INHERITS_KT_PRIVATE_CLASS!>J3()<!>.foo()<!>
}
