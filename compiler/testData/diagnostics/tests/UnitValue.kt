// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun test() {
  return checkSubtype<Unit>(Unit)
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
nullableType, typeParameter, typeWithExtension */
