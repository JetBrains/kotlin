// FIR_IDENTICAL
package test

import dependency.A

public open class B() : A() {

}

public open class C() : B() {
  <caret>
}