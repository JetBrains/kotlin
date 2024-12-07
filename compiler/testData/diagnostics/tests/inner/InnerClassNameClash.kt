// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package test

class B {
    class B {
      fun foo(b: B.C) {
      }
      class C {
      }
    }
}
