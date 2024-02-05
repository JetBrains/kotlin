// RUN_PIPELINE_TILL: FRONTEND

@DataArgument class Arguments(val enabled: Boolean = true, val size: Int = 10)

fun button(text: String, data arguments: Arguments) { }
fun helloButton(data arguments: Arguments) = button("Hello", *arguments)

fun main() {
    button("Hello", enabled = true, size = 2)
    button("Hello", size = 2, enabled = true)
    button("Hello", enabled = true)
    button("Hello")

    button("Hello", enabled = true, <!ARGUMENT_PASSED_TWICE!>enabled<!> = false)
    button("Hello", <!NAMED_PARAMETER_NOT_FOUND!>incorrect<!> = 3)
    button("Hello", enabled = <!ARGUMENT_TYPE_MISMATCH!>3<!>)

    val arguments = Arguments(enabled = true, size = 2)
    button("Hello", *arguments)
    button("Hello", <!DATAARG_WITHOUT_SPREAD!>arguments<!>)
    button("Hello", <!DATAARG_SPREAD_AND_NON_SPREAD!>*arguments<!>, enabled = true)
}

@DataArgument <!INCORRECT_DATAARG_CLASS!>class WrongDataArgument<!>(val enabled: Boolean, val size: Int = 10)

fun strange(text: String, <!DATAARG_PARAMETER_WRONG_CLASS!>data<!> arguments: Int) { }

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, primaryConstructor,
propertyDeclaration, stringLiteral */
