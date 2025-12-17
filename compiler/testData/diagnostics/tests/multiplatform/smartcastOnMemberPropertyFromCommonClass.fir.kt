// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
class Some {
    val e: SomeEnum? = null
}

enum class SomeEnum {
    A, B
}

// MODULE: main()()(common)
fun Some.test() {
    if (e == null) return
    val x = <!WHEN_ON_SEALED_GEEN_ELSE!>when (e) {
        SomeEnum.A -> "a"
        SomeEnum.B -> "B"
    }<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, enumDeclaration, enumEntry, equalityExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral,
whenExpression, whenWithSubject */
