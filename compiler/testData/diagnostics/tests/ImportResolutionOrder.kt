// FILE: _.kt
// KT-355 Resolve imports after all symbols are built

package a
  import b.*
  val x : X = X()

// FILE: _.kt
package b
  class X() {

  }

// FILE: _.kt
package c
  import d.X
  val x : X = X()

// FILE: _.kt
package d
  class X() {

  }
