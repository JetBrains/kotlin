// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class Coll {
  operator fun iterator(): It? = null
}

class It {
  operator fun next() = 1
  operator fun hasNext() = false
}

fun test() {
  for (x in <!HAS_NEXT_FUNCTION_NONE_APPLICABLE, NEXT_NONE_APPLICABLE!>Coll()<!>) {}
}

/* GENERATED_FIR_TAGS: classDeclaration, forLoop, functionDeclaration, integerLiteral, localProperty, nullableType,
operator, propertyDeclaration */
