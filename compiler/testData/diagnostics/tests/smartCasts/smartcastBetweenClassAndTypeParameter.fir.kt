// FULL_JDK
// WITH_REFLECT
// ISSUE: KT-58814

open class A
class B : A()
class C : A()

fun <T : A> createObj(implementedBy: Class<T>): T {
    val obj = when (implementedBy) {
        B::class.java -> B()
        else -> throw Exception("unsupported class")
    }
    val castObj = implementedBy.cast(obj)
    return castObj // should be OK
}