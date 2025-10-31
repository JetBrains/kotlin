// RUN_PIPELINE_TILL: FRONTEND
open class S(val a: Any, val b: Any, val c: Any) {}

interface A {
    companion object : S(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop1<!>, <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop2<!>, <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>func()<!>) {
        val prop1 = 1
        val prop2: Int
            get() = 1
        fun func() {}
    }
}

class B {
    companion object : S(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop1<!>, <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>prop2<!>, <!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>func()<!>) {
        val prop1 = 1
        val prop2: Int
            get() = 1
        fun func() {}
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, getter, integerLiteral,
interfaceDeclaration, objectDeclaration, primaryConstructor, propertyDeclaration */
