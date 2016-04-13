// TODO: remove the suppression once data classes can have supertypes
@file:Suppress("DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES")

abstract class Base {
    final override fun toString() = "OK"
    final override fun hashCode() = 42
    final override fun equals(other: Any?) = false
}

data class DataClass(val field: String) : Base()

fun box(): String {
    val d = DataClass("x")

    if (d.toString() != "OK") return "Fail toString"
    if (d.hashCode() != 42) return "Fail hashCode"
    if (d.equals(d) != false) return "Fail equals"

    return "OK"
}
