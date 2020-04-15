// IGNORE_BACKEND: JS_IR
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_REFLECT

import kotlin.reflect.*

object Host {
    fun foo(x: String) = x
}

class CompanionOwner {
    companion object {
        fun bar(x: String) = x
    }
}

fun box(): String =
        (Host::foo).call("O") + (CompanionOwner.Companion::bar).call("K")