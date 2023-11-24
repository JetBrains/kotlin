// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6, NATIVE
// FIR status: KotlinNothingValueException from create()

fun <T : A> create(modelClass: Class<T>): T {
    return if (modelClass.isAssignableFrom(B::class.java)) {
        createViewModel()
    } else {
        throw Exception()
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : A> createViewModel(): T {
    return B() as T
}

open class A
class B : A()

fun box(): String {
    val r = create(A::class.java)
    return if (r is B) "OK" else "fail"
}