// RUN_PIPELINE_TILL: BACKEND
class A(val foo: Int)

fun A.test(foo: String) {
    val a: String = foo
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty,
primaryConstructor, propertyDeclaration */
