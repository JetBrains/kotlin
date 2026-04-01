// RUN_PIPELINE_TILL: FRONTEND
open class S(val a: Any, val b: Any, val c: Any) {}

interface A {
    companion object : S(<!UNRESOLVED_REFERENCE!>prop1<!>, <!UNRESOLVED_REFERENCE!>prop2<!>, <!UNRESOLVED_REFERENCE!>func<!>()) {
        val prop1 = 1
        val prop2: Int
            get() = 1
        fun func() {}
    }
}

class B {
    companion object : S(<!UNRESOLVED_REFERENCE!>prop1<!>, <!UNRESOLVED_REFERENCE!>prop2<!>, <!UNRESOLVED_REFERENCE!>func<!>()) {
        val prop1 = 1
        val prop2: Int
            get() = 1
        fun func() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, integerLiteral,
interfaceDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration */
