// RUN_PIPELINE_TILL: FRONTEND

fun <A> A.mutate(block: (A) -> A): A = block(this)

class Person(
    copy var name: String,
    copy var title: String?,
) {
    copy fun f() { }
}

copy fun Person.g() { }

fun test(p: Person) {
    p.mutate(Person::f)
    p.mutate(Person::g)
    // p.mutate { p -> p }
    // p.mutate { copy p -> }
    // p.mutate { copy p ->
    //   p.name = "you"
    // }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, nullableType, primaryConstructor, propertyDeclaration, thisExpression */
