// RUN_PIPELINE_TILL: BACKEND
 class Foo {
     fun test() {
         fun Foo.bar() {}
         bar()
     }
 }

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, localFunction */
