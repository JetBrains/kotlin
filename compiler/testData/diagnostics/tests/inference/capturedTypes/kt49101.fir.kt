// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: FRONTEND
class A<T: B<out Number>>(val x: T) {
    fun test() {
        val y: Int = x.m<<!UPPER_BOUND_VIOLATED, UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>C<out Number><!>>()
    }

}

class B<T1> {
    fun <X1: C<T1>> m(): Int = 1
}

class C<T>

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, nullableType, outProjection,
primaryConstructor, propertyDeclaration, typeConstraint, typeParameter */
