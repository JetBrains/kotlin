// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlin.Self

sealed interface Base {
   fun foo(): Int {
       return when(this) {
           is A -> 1
           is B<*> -> 2
       }
   }
}

class A : Base
@Self
class B : Base

fun box(): String {
    return if (B().foo() == 1) "ERROR" else "OK"
}