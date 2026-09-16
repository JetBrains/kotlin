// RUN_PIPELINE_TILL: CODEGEN
class Foo

operator fun Foo.invoke() {}

fun foo() {
    val x = Foo()

    x()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localProperty, operator,
propertyDeclaration */
