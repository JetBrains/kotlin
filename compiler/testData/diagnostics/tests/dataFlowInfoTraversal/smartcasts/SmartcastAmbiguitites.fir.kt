// RUN_PIPELINE_TILL: FRONTEND
interface B {
  fun bar() {}
}

class C() {
  fun bar() {
  }
}

fun test(a : Any?) {
  if (a is B) {
      if (<!IMPOSSIBLE_IS_CHECK_ERROR!>a is C<!>) {
          a.bar();
      }
  }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, ifExpression, interfaceDeclaration, intersectionType,
isExpression, nullableType, primaryConstructor, smartcast */
