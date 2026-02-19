// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-78595

val <T: Any> T.javaKlass: java.lang.Class<T>
    get() {
        this as <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Object<!>
        return <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>getClass<!>() <!UNCHECKED_CAST!>as java.lang.Class<T><!>
    }

fun consume(arg: java.lang.Class<*>) {}

interface TypeA
interface TypeB

fun test(a: TypeA, b: TypeB) {
    if (a.javaKlass == b.javaKlass) {
        consume(a.javaKlass)
    }
}

/* GENERATED_FIR_TAGS: asExpression, equalityExpression, flexibleType, functionDeclaration, getter, ifExpression,
interfaceDeclaration, intersectionType, javaFunction, propertyDeclaration, propertyWithExtensionReceiver, smartcast,
starProjection, thisExpression, typeConstraint, typeParameter */
