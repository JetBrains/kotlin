// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Parent
interface Child : Parent

sealed class Page : Parent {
  object One : Page(), Child
  object Two : Page(), Child
}

// Ok: page is a Parent so it can be easily a Child
fun test(page: Page): Boolean = page is Child

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, nestedClass,
objectDeclaration, sealed */
