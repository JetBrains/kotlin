// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

class Person(
    copy var name: String,
    copy var title: String?,
) {
    copy fun f() { }
}

fun test() {
    var person = Person("me", null)
    person.f()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, thisExpression */
