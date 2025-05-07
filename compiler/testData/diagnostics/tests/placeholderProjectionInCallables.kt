// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76914

interface Box<V> {
    val property: V
}

class BoxClass<V>

fun main() {
    Box<<!UNRESOLVED_REFERENCE!>_<!>>::property

    val a: (Box<Int>) -> Int = Box<<!UNRESOLVED_REFERENCE!>_<!>>::property
}
