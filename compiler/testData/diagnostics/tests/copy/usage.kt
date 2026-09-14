// RUN_PIPELINE_TILL: FRONTEND

class Person(
    copy var name: String,
    copy var title: String?,
    var age: Int,
) {
    copy fun f() { }
}

fun person(): Person = Person("me", null, 1)

fun test() {
    <!COPY_PATH_UNSUPPORTED_EXPRESSION!>person()<!>.f()

    var p1 = Person("me", null, 1)
    <!COPY_PATH_WRONG_STEP!>p1<!>.f()
    <!COPY_PATH_WRONG_STEP!>p1<!>.name = "you"
    p1.age = 3

    copy var p2 = Person("me", null, 1)
    p2.f()
    p2.name = "you"
    p2.<!COPY_PATH_WRONG_STEP!>age<!> = 3

    p2.f().<!UNRESOLVED_REFERENCE!>f<!>()
}

fun Person.test2() {
    <!COPY_PATH_WRONG_STEP!>f()<!>
    <!COPY_PATH_WRONG_STEP!>name = "you"<!>
    age = 3
}

copy fun Person.test3() {
    f()
    name = "you"
    <!COPY_PATH_WRONG_STEP!>age<!> = 3
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, nullableType, primaryConstructor,
propertyDeclaration, stringLiteral, thisExpression */
