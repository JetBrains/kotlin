// RUN_PIPELINE_TILL: FRONTEND
annotation class Ann(val a: String)

const val bool = false
const val string = "string"

@Ann(
    <!ANNOTATION_ARGUMENT_WITH_CONTROL_FLOW_NOT_SUPPORTED!>if (true) "A" else "B"<!>
)
fun test1() {}

@Ann(
    "A" + <!ANNOTATION_ARGUMENT_WITH_CONTROL_FLOW_NOT_SUPPORTED!>if (bool) "B" else "C" + "D"<!>
)
fun test2() {}

@Ann(
    <!ANNOTATION_ARGUMENT_WITH_CONTROL_FLOW_NOT_SUPPORTED!>when {
        string == "A" -> "1"
        bool -> "2"
        else -> "3"
    }<!>
)
fun test3() {}


/* GENERATED_FIR_TAGS: additiveExpression, annotationDeclaration, classDeclaration, collectionLiteral, const,
functionDeclaration, integerLiteral, outProjection, primaryConstructor, propertyDeclaration, stringLiteral, vararg */
