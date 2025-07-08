// RUN_PIPELINE_TILL: FRONTEND

class MyClass
class SomeClass

operator fun SomeClass.component1() {}
operator fun SomeClass.component2() {}

fun test() {
    val (o, o2) = SomeClass()
    val (o3, o4) = <!COMPONENT_OPERATOR_MISSING, COMPONENT_OPERATOR_MISSING!>MyClass()<!>

}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, funWithExtensionReceiver, functionDeclaration,
localProperty, operator, propertyDeclaration */
