// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

class A(val a:Int) {

  inner class B() {
    val x = checkSubtype<B>(<!DEBUG_INFO_LEAKING_THIS!>this@B<!>)
    val y = checkSubtype<A>(this@A)
    val z = checkSubtype<B>(<!DEBUG_INFO_LEAKING_THIS!>this<!>)
    val Int.xx : Int get() = checkSubtype<Int>(this)
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, getter, infix,
inner, nullableType, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver, thisExpression,
typeParameter, typeWithExtension */
