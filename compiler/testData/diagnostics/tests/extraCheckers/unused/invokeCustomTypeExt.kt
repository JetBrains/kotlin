// RUN_PIPELINE_TILL: BACKEND
class Foo

operator fun Foo.invoke() {}

fun foo() {
    val x = Foo()

    x()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration */
