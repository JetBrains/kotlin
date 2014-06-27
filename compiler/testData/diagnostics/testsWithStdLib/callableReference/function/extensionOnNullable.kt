class A {
    fun foo() {}
}

fun A?.foo() {}

val f = A::foo : KMemberFunction0<A, Unit>
val g = A?::foo : KExtensionFunction0<A, Unit>
