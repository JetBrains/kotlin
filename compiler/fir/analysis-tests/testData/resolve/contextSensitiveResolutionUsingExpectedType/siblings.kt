// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed class MyClass {
    object NestedInheritor : MyClass()
}

fun receiveMyClass(m: MyClass) {}

fun sealed1(b: Boolean, myClass: MyClass) {
    receiveMyClass(
        when {
            b -> myClass
            else -> NestedInheritor
        }
    )
}

fun sealed2(b: Boolean, subClass: MyClass.NestedInheritor) {
    receiveMyClass(
        when {
            b -> subClass
            else -> NestedInheritor
        }
    )
}

fun sealed3(b1: Boolean, b2: Boolean, subClass: MyClass.NestedInheritor) {
    receiveMyClass(
        when {
            b1 -> subClass
            b2 -> NestedInheritor
            else -> <!UNRESOLVED_REFERENCE!>OtherInheritor<!>
        }
    )
}
