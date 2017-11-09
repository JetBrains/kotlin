// WITH_RUNTIME

val test1 = 0

val test2: Int get() = 0

var test3 = 0

var test4 = 1; set(value) { field = value }

var test5 = 1; private set

val test6 = 1; get

val test7 by lazy { 42 }

var test8 by hashMapOf<String, Int>()
