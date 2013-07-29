class A<out X: Comparable<X>, in <caret>Y: Number, Z>

val a = A<String, Int, Any>()