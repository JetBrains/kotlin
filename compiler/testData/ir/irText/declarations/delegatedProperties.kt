// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// KT-61141: kotlin.collections.HashMap instead of java.util.HashMap
// IGNORE_BACKEND: NATIVE

val test1 by lazy { 42 }

class C(val map: MutableMap<String, Any>) {
    val test2 by lazy { 42 }
    var test3 by map
}

var test4 by hashMapOf<String, Any>()
