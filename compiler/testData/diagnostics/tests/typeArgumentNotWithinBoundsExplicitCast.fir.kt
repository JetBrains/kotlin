// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-78304

interface Consumer<T: Consumer<T>> {
    fun consume(item: T) {}
}

class Impl: Consumer<Impl>

fun main() {
    val e = Impl() <!UNCHECKED_CAST!>as Consumer<Any><!>
    e.consume(e)
}

fun accept(consumer: Consumer<<!UPPER_BOUND_VIOLATED!>Any<!>>) {}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, interfaceDeclaration, localProperty,
propertyDeclaration, typeConstraint, typeParameter */
