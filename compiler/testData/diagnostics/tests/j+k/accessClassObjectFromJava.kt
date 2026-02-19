// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class Foo {
  companion object {
    val bar = 1

    fun test(a: Foo.<!UNRESOLVED_REFERENCE!>`object`<!>) {

    }

  }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, integerLiteral, objectDeclaration,
propertyDeclaration */
