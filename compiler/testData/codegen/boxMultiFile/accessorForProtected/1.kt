import b.B
import a.BSamePackage

fun box() = if (B().test() == BSamePackage().test()) "OK" else "fail"
