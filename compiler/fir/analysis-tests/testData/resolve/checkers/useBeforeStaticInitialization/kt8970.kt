// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
abstract class A(val x : Any?)
object B : A(<!UNINITIALIZED_ACCESS!>C<!>)
object C : A(<!UNINITIALIZED_ACCESS!>B<!>)

abstract class Base(val x: Any?)

class C1 {
    companion object : Base(<!UNINITIALIZED_ACCESS!>C2<!>)
}

class C2 {
    companion object : Base(<!UNINITIALIZED_ACCESS!>C1<!>)
}
