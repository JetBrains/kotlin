// RUN_PIPELINE_TILL: FRONTEND

class Person(
    copy var name: String,
    copy var title: String?,
    var age: Int,
) {
    copy fun f() { }
}

fun <A> A.freshen(): A = this

copy fun Person.g() {
    this.f()
    this.name = "me"

    copy var x = this

    <!COPY_PATH_ALIASED!>this<!>.f()
    <!COPY_PATH_ALIASED!>x<!>.f()
    <!COPY_PATH_ALIASED!>this<!>.name = "me"
    <!COPY_PATH_ALIASED!>x<!>.name = "me"

    copy var y = x

    copy var z = x.freshen()
    <!COPY_PATH_ALIASED!>this<!>.f()
    z.f()
    <!COPY_PATH_ALIASED!>this<!>.name = "me"
    z.name = "me"

    copy var w = z.name
    <!COPY_PATH_ALIASED!>z.name<!> = "me"

    copy var u = z
    <!COPY_PATH_ALIASED!>z<!>.f()
    <!COPY_PATH_ALIASED!>u<!>.f()
    <!COPY_PATH_ALIASED!><!COPY_PATH_ALIASED!>z<!>.name<!> = "me"
    <!COPY_PATH_ALIASED!>u<!>.name = "me"
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty, nullableType,
primaryConstructor, propertyDeclaration, thisExpression */
