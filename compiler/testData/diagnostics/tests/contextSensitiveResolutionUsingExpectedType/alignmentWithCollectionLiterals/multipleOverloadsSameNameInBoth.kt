// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-89232
// FIR_DUMP

enum class MyEnum1 { X, Y }
enum class MyEnum2 { X, Z }
enum class MyEnum3 { W }

fun foo(x: MyEnum1) {}
fun foo(x: MyEnum2) {}

fun bar(x: MyEnum1) {}
fun bar(x: MyEnum3) {}

fun baz(x: MyEnum1) {}
fun baz(x: String) {}

fun <T> id(x: T): T = x

fun main() {
    // Declared in both enums
    foo(<!UNRESOLVED_REFERENCE!>X<!>)
    // Declared only in one of the enums
    foo(<!UNRESOLVED_REFERENCE!>Y<!>)
    foo(<!UNRESOLVED_REFERENCE!>Z<!>)
    bar(<!UNRESOLVED_REFERENCE!>X<!>)
    bar(<!UNRESOLVED_REFERENCE!>W<!>)
    // The second overload is not enum-based
    baz(<!UNRESOLVED_REFERENCE!>X<!>)
    baz(<!UNRESOLVED_REFERENCE!>Y<!>)
    // Declared nowhere
    foo(<!UNRESOLVED_REFERENCE!>Q<!>)

    // Type variable in between
    bar(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>W<!>))
    baz(<!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>Y<!>))
}

fun localOverloads() {
    fun local(x: MyEnum1) {}
    fun local(x: MyEnum3) {}

    local(<!UNRESOLVED_REFERENCE!>Y<!>)
    local(<!UNRESOLVED_REFERENCE!>W<!>)
    local(<!UNRESOLVED_REFERENCE!>X<!>)
}

/* GENERATED_FIR_TAGS: enumDeclaration, enumEntry, functionDeclaration, localFunction, nullableType, typeParameter */
