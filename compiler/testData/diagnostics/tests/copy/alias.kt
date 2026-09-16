// RUN_PIPELINE_TILL: FRONTEND

class Person(
    copy var name: String,
    copy var title: String?,
    var age: Int,
) {
    copy fun f() { }
}

inline fun <A> A.freshen(): A = this

copy fun Person.g() {
    copy var x = this
    copy var y = x

    copy var z = x.freshen()
    copy var w = z.name
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty, nullableType,
primaryConstructor, propertyDeclaration, thisExpression */
