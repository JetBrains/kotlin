// RUN_PIPELINE_TILL: BACKEND
interface D {
  fun foo(): String = ""
}

fun test(d: Any?) {
  if (d !is D) return

  class Local {
    fun f() {
      <!DEBUG_INFO_SMARTCAST!>d<!>.foo()
    }

    fun f1() = <!DEBUG_INFO_SMARTCAST!>d<!>.foo()

    fun f2(): String = <!DEBUG_INFO_SMARTCAST!>d<!>.foo()
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, isExpression,
localClass, nullableType, smartcast, stringLiteral */
