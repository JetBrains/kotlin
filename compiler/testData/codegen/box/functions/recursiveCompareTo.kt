// IGNORE_BACKEND_FIR: JVM_IR
class C

operator fun C.compareTo(o: C) : Int {
    if (this == o) return 0
    if (o >= o) {
        return 1
    }
    return -1
}

fun box() : String = if (C() > C()) "OK" else "FAIL"