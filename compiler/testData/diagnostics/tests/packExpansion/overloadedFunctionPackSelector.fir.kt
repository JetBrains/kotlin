// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

class Props {
    val color: String = ""
}

fun Text(text: String, ...Props.$props) {}

fun Text(value: Int, ...Props.$props) {}

fun Wrapper(<!OVERLOAD_RESOLUTION_AMBIGUITY!>...Text.$props<!>) {
    <!UNRESOLVED_REFERENCE!>text<!>
    <!UNRESOLVED_REFERENCE!>color<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, propertyDeclaration, stringLiteral */
