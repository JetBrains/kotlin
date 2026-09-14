// RUN_PIPELINE_TILL: FRONTEND
// FIR_DUMP

class Person(
    copy var name: String,
    copy var title: String?,
) {
    copy var visibleName: String = "hello"

    copy fun f() { }

    <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>copy fun f1() = f()<!>
    copy fun f2(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Unit<!> { }
    copy fun f3(): <!COPY_FUN_WITH_RETURN_TYPE_OR_EXPRESSION_BODY!>Int<!> { }

    fun test() {
        val h = { copy x : Person -> 0 }
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

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, integerLiteral, lambdaLiteral,
localProperty, nullableType, primaryConstructor, propertyDeclaration */
