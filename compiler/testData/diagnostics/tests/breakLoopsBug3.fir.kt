// ISSUE: KT-71966

package a

abstract class A : C() {
    abstract class Nested
}

abstract class C : A.Nested()
