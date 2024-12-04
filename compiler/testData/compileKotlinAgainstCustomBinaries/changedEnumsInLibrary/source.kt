import test.*

fun f1(x: E1) = when (x) {
    E1.A -> "A"
    E1.B -> "B"
    E1.C -> "C"
}

fun f2(x: E2) = when (x) {
    E2.A -> "A"
    E2.B -> "B"
    E2.C -> "C"
}

fun run(): String {
    val c2 = try { f2(E2.C) } catch (e: java.lang.NoSuchFieldError) { "" }
    return f1(E1.A) + f1(E1.B) + f1(E1.C) + f2(E2.A) + f2(E2.B) + c2
}