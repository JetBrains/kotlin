// KT-5943 StackOverflowError from commonSupertype of two enums

enum class A { A }
enum class B { B }

val x = if (true) A.A else B.B