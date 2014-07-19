trait A {
  fun foo() {}
}

trait B : A {}

class C(b : B) : B by b {

}

