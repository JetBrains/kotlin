// RUN_PIPELINE_TILL: FRONTEND

@SealedArgument sealed interface Argument
@JvmInline value class Number(val n: Int): Argument
@JvmInline value class String(val s: kotlin.String): Argument

fun foo(sealed arg: Argument, other: Boolean) { }
fun bar(sealed arg: Argument) = foo(*arg, true)

fun main() {
    foo(1, false)
    foo("Hello", false)
    foo(<!SEALED_ARGUMENT_NO_CONSTRUCTOR!>true<!>, false)

    foo(*Number(2), false)
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!>(*3, false)
}

<!INCORRECT_SEALEDARG_CLASS!>@SealedArgument sealed interface IncorrectArgument<!>
data class OneNumber(val n: Int): IncorrectArgument
data class OtherNumber(val m: Int): IncorrectArgument

fun bar(<!SEALEDARG_PARAMETER_WRONG_CLASS!>sealed<!> arg: Int) { }

/* GENERATED_FIR_TAGS: classDeclaration, data, functionDeclaration, integerLiteral, interfaceDeclaration,
primaryConstructor, propertyDeclaration, sealed, stringLiteral, value */
