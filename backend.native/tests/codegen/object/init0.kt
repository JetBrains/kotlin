package codegen.`object`.init0

import kotlin.test.*

class A(a:Int) {
  var i:Int = 0
  init {
    if (a == 0) i = 1
  }
}