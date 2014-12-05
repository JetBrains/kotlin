package test

class B {
    class B {
      fun foo(<!UNUSED_PARAMETER!>b<!>: B.C) {
      }
      class C {
      }
    }
}