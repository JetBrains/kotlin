// LANGUAGE: +CollectionLiterals, +ContextSensitiveResolutionUsingExpectedType
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@RequiresOptIn("", level = ERROR)
annotation class MyExperimental

enum class E {
    @MyExperimental A, B;
    companion object {
        @MyExperimental
        operator fun of(vararg es: String): E = B
    }
}

sealed class S {
    class SC: S()
    @MyExperimental
    object SO: S()
}

fun test(e: S) {
    val a: E = @OptIn(MyExperimental::class) A
    val b: E = @OptIn(MyExperimental::class) ["kodie"]

    val c: E = @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!OPT_IN_USAGE_ERROR!>A<!>
    val d: E = @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!OPT_IN_USAGE_ERROR!>["kodie"]<!>

    if (e is @<!UNRESOLVED_REFERENCE!>Unresolved<!> SC) {

    }

    val f: S = @OptIn(MyExperimental::class) SO
    val g: S = @<!UNRESOLVED_REFERENCE!>Unresolved<!> <!OPT_IN_USAGE_ERROR!>SO<!>
}

/* GENERATED_FIR_TAGS: annotationDeclaration, companionObject, enumDeclaration, enumEntry, functionDeclaration,
localProperty, objectDeclaration, operator, propertyDeclaration, stringLiteral, vararg */
