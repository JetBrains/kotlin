// RUN_PIPELINE_TILL: BACKEND
// KT-668 Failed to resolve generic parameter
open class A()
open class B() : A() {
  fun b(): B = B()
}


class C() {
  fun <T> a(x: (T)->T, y: T): T {
    return x(x(y))
  }

  val x: B = a({it.b()}, B())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, functionalType, lambdaLiteral, nullableType,
primaryConstructor, propertyDeclaration, typeParameter */
