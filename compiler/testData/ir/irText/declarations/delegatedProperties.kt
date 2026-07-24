// WITH_STDLIB
// DUMP_IR_DIFFERENCE: JVM
//   K/JVM uses actualized java.util.HashMap instead of kotlin.collections.HashMap

val test1 by lazy { 42 }

class C(val map: MutableMap<String, Any>) {
    val test2 by lazy { 42 }
    var test3 by map
}

var test4 by hashMapOf<String, Any>()
