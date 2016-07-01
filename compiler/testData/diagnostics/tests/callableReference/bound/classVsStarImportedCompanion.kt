// FILE: 1.kt

package a

import b.B.*
import kotlin.reflect.KClass

class Companion

val f: KClass<a.Companion> = Companion::class

// FILE: 2.kt

package b

class B {
    companion object Companion
}
