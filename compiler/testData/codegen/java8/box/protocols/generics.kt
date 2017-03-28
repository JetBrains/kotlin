// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

/* Test ideas:
 * 1. P<out X> = C<out Y <: X>
 * 2. P<in X> = C<in Y :> X>
 * 3. P<X> = C<X>
 * 4. P<X> = C
 * 5. P = C<X>
 */

protocol interface P {
    fun x()
}

protocol interface PT<T> {
    fun x(t: T): T
}

protocol interface PTI<in T> {
    fun x(t: T)
}

protocol interface PTO<out T> {
    fun x(): T
}

class X {
    fun x() {}
    fun x(i: Int) = i
    fun x(i: Int?) = i!! * 2
}

fun box(): String {
    val obj: PT<Int> = X()
    if (obj.x(42) != 42) {
        return "FAIL PRIMITIVE"
    }

    val obj2: PT<Int?> = X()
    if (obj2.x(42) != 84) {
        return "FAIL OBJECT"
    }

    return "OK"
}