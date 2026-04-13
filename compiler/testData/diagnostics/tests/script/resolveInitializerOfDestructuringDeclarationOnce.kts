// RUN_PIPELINE_TILL: FRONTEND
val (a, b, c) = A<!NO_VALUE_FOR_PARAMETER!>()<!>

class A(val a: Int) {
    operator fun component1() {}
    operator fun component2() {}
    operator fun component3() {}
}

/* GENERATED_FIR_TAGS: classDeclaration, destructuringDeclaration, functionDeclaration, localProperty, operator,
primaryConstructor, propertyDeclaration */
