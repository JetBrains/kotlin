// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-55055
// FIR_DUMP
fun <T : Number> printGenericNumber(t: T) = println("Number is $t")

fun main() {
    buildList { // inferred into MutableList<String>
        add("Boom")
        <!CANNOT_INFER_PARAMETER_TYPE!>printGenericNumber<!>(<!ARGUMENT_TYPE_MISMATCH!>this[0]<!>)
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, stringLiteral, thisExpression, typeConstraint,
typeParameter */
