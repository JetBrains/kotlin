// FILE: b.kt
// KT-355 Resolve imports after all symbols are built

package a
  import b.*
  val x : X = X()

// FILE: b.kt
package b
  class X() {

  }

// FILE: b.kt
package c
  import d.X
  val x : X = X()

// FILE: b.kt
package d
  class X() {

  }
