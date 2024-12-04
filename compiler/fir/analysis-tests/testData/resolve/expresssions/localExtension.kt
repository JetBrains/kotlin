// RUN_PIPELINE_TILL: BACKEND
 class Foo {
     fun test() {
         fun Foo.bar() {}
         bar()
     }
 }
