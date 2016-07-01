// FILE: 1.kt

package a

import b.*
import kotlin.reflect.KClass

class A
object B

val f: KClass<a.A> = A::class
val g: KClass<a.B> = B::class

// FILE: 2.kt

package b

object A
object B
