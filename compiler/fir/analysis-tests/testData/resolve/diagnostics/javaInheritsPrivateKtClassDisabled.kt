// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_PHASE_SUGGESTION: java.lang.IllegalStateException: No override for FUN IR_EXTERNAL_JAVA_DECLARATION_STUB name:equals visibility:public modality:OPEN <> ($this:kotlin.Any, other:kotlin.Any?) returnType:kotlin.Boolean [operator] in CLASS CLASS name:K2 modality:OPEN visibility:private superTypes:[<root>.K1]
// ISSUE: KT-66328
// LANGUAGE: -ProhibitJavaClassInheritingPrivateKotlinClass
// FILE: J1.java

public class J1 extends K2 {}

// FILE: J2.java
public class J2 extends J1 {
    public void foo() {}
}

// FILE: kotlin.kt
private open class K2 : K1() {}

open class K1 {}

class K3 : J2() {}

class K4 : J2 {
    constructor() : super() {}
}

fun foo(): Unit {
    val a = J1()
    val b = K3()
    b.foo()
    val c = J2()
    c.foo()
}
