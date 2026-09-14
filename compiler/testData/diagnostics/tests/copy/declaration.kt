// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

class Person(
    copy var name: String,
    copy var title: String?,
    <!COPY_VAL!>copy val age: Int?<!>,
) {
    <!COPY_VAR_UNSUPPORTED!>copy var visibleName: String = "hello"<!>

    copy fun f() { }

    <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>copy fun f1() = f()<!>
    copy fun f2(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Unit<!> { }
    copy fun f3(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Int<!> { }

    <!COPY_FUN_TOO_MANY_ARGS!>copy fun f4(copy x: Int) { }<!>

    fun test() {
        val h1 = { copy x : Person -> 0 }
        val h2 = { copy x : Person, y : Int -> 0 }
        val h3 = <!COPY_FUN_TOO_MANY_ARGS!>{ copy x : Person, copy y : Int -> 0 }<!>

        copy var one: Int = 1
        <!COPY_VAL!>copy val two: Int = 2<!>
    }
}

copy fun Person.g() { }

<!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>copy fun Person.g1() = g()<!>
copy fun Person.g2(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Unit<!> { }
copy fun Person.g3(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Int<!> { }

<!NO_THIS!>copy fun g4() { }<!>
<!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY, NO_THIS!>copy fun g5() = TODO()<!>
<!NO_THIS!>copy fun g6(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Unit<!> { }<!>
<!NO_THIS!>copy fun g7(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Int<!> { }<!>

<!COPY_FUN_TOO_MANY_ARGS!>copy fun Person.g8(copy x: Int) { }<!>

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration */
