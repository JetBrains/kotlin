// IGNORE_BACKEND_FIR: JVM_IR
// See KT-6206 Always generate hashCode() and equals() for data classes even if base classes have non-trivial analogs

abstract class Base {
    override fun toString() = "Fail"
    override fun hashCode() = -42
    override fun equals(other: Any?) = false
}

data class DataClass(val field: String) : Base()

fun box(): String {
    val d = DataClass("x")

    if (d.toString() != "DataClass(field=x)") return "Fail toString"
    if (d.hashCode() != "x".hashCode()) return "Fail hashCode"
    if (d.equals(d) == false) return "Fail equals"

    return "OK"
}
