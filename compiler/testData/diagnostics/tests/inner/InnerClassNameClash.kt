// RUN_PIPELINE_TILL: BACKEND
package test

class B {
    class B {
      fun foo(b: B.C) {
      }
      class C {
      }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nestedClass */
