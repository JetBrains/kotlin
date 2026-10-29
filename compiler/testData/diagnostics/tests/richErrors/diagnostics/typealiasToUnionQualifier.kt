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
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>CR<!>.<!UNRESOLVED_REFERENCE!>c<!>()
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>CR<!>.<!UNRESOLVED_REFERENCE!>p<!>
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>CR<!>.<!UNRESOLVED_REFERENCE!>Nested<!>()
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>CR<!>.<!UNRESOLVED_REFERENCE!>O<!>
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>CR<!>
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>JR<!>.<!UNRESOLVED_REFERENCE!>s<!>()
    <!TYPEALIAS_TO_UNION_TYPE_AS_QUALIFIER!>JR<!>.<!UNRESOLVED_REFERENCE!>FIELD<!>
    x.ext()
    x.extProp
    val f: (CR) -> Unit = CR::ext
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, nestedClass, objectDeclaration, propertyDeclaration, propertyWithExtensionReceiver, typeAliasDeclaration */
