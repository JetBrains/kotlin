// WITH_RUNTIME

val test1 by lazy { 42 }

class C(val map: MutableMap<String, Any>) {
    val test2 by lazy { 42 }
    var test3 by map
}

var test4 by hashMapOf<String, Any>()