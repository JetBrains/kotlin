package test

class A {
    companion object
}

object O

enum class E {
    ENTRY
}


val a0 = A.javaClass
val a1 = test.A.javaClass
val a2 = A.Companion.javaClass
val a3 = A::class.java
val a4 = test.A::class.java
val a5 = A.Companion::class.java

val o0 = O.javaClass
val o1 = O::class.java

val e0 = E.javaClass
val e1 = E::class.java
val e2 = E.ENTRY.javaClass

val int0 = Int.javaClass
val int1 = Int::class.java

val string0 = String.javaClass
val string1 = String::class.java
