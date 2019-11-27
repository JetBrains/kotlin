// IGNORE_BACKEND_FIR: JVM_IR
var log = ""

fun foo(): Int {
    log += "foo;"
    return 1
}
fun bar(): Int {
    log += "bar;"
    return 42
}

data class A(val x: Int) {
    operator fun plus(other: A) = A(x + other.x)
}

fun box(): String {
    val array = arrayOf(0, 1)
    array[foo()] += bar()

    if (array[0] != 0) return "fail1a: ${array[0]}"
    if (array[1] != 43) return "fail1b: ${array[0]}"

    log += "!;"

    val objArray = arrayOf(A(0), A(1))
    objArray[foo()] += A(bar())
    if (objArray[0] != A(0)) return "fail2a: ${array[0]}"
    if (objArray[1] != A(43)) return "fail2b: ${array[0]}"

    if (log != "foo;bar;!;foo;bar;") return "fail3: $log"

    return "OK"
}