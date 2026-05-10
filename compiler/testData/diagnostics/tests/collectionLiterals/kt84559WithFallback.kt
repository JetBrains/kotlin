// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +CollectionLiterals
// WITH_STDLIB

@RequiresOptIn("", level = RequiresOptIn.Level.ERROR)
annotation class MyExperimental

@MyExperimental
object A

fun test() {
    val lst1 = @<!UNRESOLVED_REFERENCE!>Unresolved<!> [42]
    val lst2 = @OptIn(MyExperimental::class) [A, A]
}

/* GENERATED_FIR_TAGS: annotationDeclaration, functionDeclaration, integerLiteral, localProperty, objectDeclaration,
propertyDeclaration, stringLiteral */
