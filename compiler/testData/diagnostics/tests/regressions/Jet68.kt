// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
class Foo()

fun test() {
  val f : Foo? = null
  if (f == null) {

  }
  if (f != null) {

  }
}

/* GENERATED_FIR_TAGS: classDeclaration, equalityExpression, functionDeclaration, ifExpression, localProperty,
nullableType, primaryConstructor, propertyDeclaration */
