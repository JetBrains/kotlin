// RUN_PIPELINE_TILL: FRONTEND
// KT-7753: attempt to call enum constructor explicitly
enum class A(val c: Int) {
    ONE(1),
    TWO(2);    
}

fun createA(): A {
    // Error should be here!
    return <!ENUM_CLASS_CONSTRUCTOR_CALL!><!INVISIBLE_REFERENCE!>A<!>(10)<!>
}
