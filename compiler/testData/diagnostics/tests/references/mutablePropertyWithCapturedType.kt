// RUN_PIPELINE_TILL: FRONTEND
class Generic<T>
class Klass<T> {
    var mutableProperty: Generic<T> = Generic()
}

fun test() {
    val mutableProperty = Klass<*>::<!MUTABLE_PROPERTY_WITH_CAPTURED_TYPE!>mutableProperty<!>
    mutableProperty.set(Klass<Int>(), <!MEMBER_PROJECTED_OUT!>Generic<String>()<!>)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, localProperty, nullableType,
outProjection, propertyDeclaration, starProjection, typeParameter */
