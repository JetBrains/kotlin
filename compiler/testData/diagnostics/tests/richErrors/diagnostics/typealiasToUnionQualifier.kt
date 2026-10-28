// RUN_PIPELINE_TILL: FRONTEND
// FILE: J.java
public class J {
    public static void s() {}
    public static int FIELD = 1;
}

// FILE: test.kt
error class E

class C {
    companion object {
        fun c() {}
        val p = 1
    }
    class Nested
    object O
}

typealias CR = C | E
typealias JR = J | E

fun CR.ext() {}
val CR.extProp: Int get() = 1

fun test(x: CR) {
    CR.<!UNRESOLVED_REFERENCE!>c<!>()
    CR.<!UNRESOLVED_REFERENCE!>p<!>
    CR.<!UNRESOLVED_REFERENCE!>Nested<!>()
    CR.<!UNRESOLVED_REFERENCE!>O<!>
    <!NO_COMPANION_OBJECT!>CR<!>
    JR.<!UNRESOLVED_REFERENCE!>s<!>()
    JR.<!UNRESOLVED_REFERENCE!>FIELD<!>
    x.ext()
    x.extProp
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, nestedClass, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, typeAliasDeclaration */
