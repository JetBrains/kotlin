// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-34064
// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS

// KT-34064: AssertionError when class implements by delegation multiple interfaces with conflicting overloads
interface A {
    fun a(arg: Int)
}

interface B {
    fun <T> a(arg: Int)
}

class AB(val a: A, val b: B) : A by a, B by b {}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, inheritanceDelegation, interfaceDeclaration, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
