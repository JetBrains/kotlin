// IGNORE_BACKEND: JVM_IR
class A {
    var result = "OK"
}

fun box() = (::A)().result
