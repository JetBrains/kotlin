// IGNORE_BACKEND_FIR: JVM_IR
// FILE: b.kt

class Foo<T>(val minus0: T, val plus0: T) {

}

fun box(): String {
    val foo = Foo<Double>(-0.0, 0.0)
    val fooF = Foo<Float>(-0.0F, 0.0F)

    if (foo.minus0 < foo.plus0) return "fail 0"
    if (fooF.minus0 < fooF.plus0) return "fail 1"

    if (foo.minus0 != foo.plus0) return "fail 3"
    if (fooF.minus0 != fooF.plus0) return "fail 4"

    return "OK"
}