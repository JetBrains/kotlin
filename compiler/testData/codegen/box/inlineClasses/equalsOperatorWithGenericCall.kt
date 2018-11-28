// IGNORE_BACKEND: JVM_IR
inline class IcAny(val x: Any?)
inline class IcInt(val x: Int)
inline class IcLong(val x: Long)

fun <T> id(x: T) = x

fun box(): String {
    if (IcInt(42) == id(IcInt(24))) return "Error 1"
    if (IcInt(42) != id(IcInt(42))) return "Error 2"
    if (id(IcInt(42)) == IcInt(24)) return "Error 3"
    if (id(IcInt(42)) != IcInt(42)) return "Error 4"
    if (id(IcInt(42)) == id(IcInt(24))) return "Error 5"
    if (id(IcInt(42)) != id(IcInt(42))) return "Error 6"

    if (IcLong(42) == id(IcLong(24))) return "Error 2, 1"
    if (IcLong(42) != id(IcLong(42))) return "Error 2, 2"
    if (id(IcLong(42)) == IcLong(24)) return "Error 2, 3"
    if (id(IcLong(42)) != IcLong(42)) return "Error 2, 4"
    if (id(IcLong(42)) == id(IcLong(24))) return "Error 2, 5"
    if (id(IcLong(42)) != id(IcLong(42))) return "Error 2, 6"

    if (IcAny(42) == id(IcAny(24))) return "Error 3, 1"
    if (IcAny(42) != id(IcAny(42))) return "Error 3, 2"
    if (id(IcAny(42)) == IcAny(24)) return "Error 3, 3"
    if (id(IcAny(42)) != IcAny(42)) return "Error 3, 4"
    if (id(IcAny(42)) == id(IcAny(24))) return "Error 3, 5"
    if (id(IcAny(42)) != id(IcAny(42))) return "Error 3, 6"

    return "OK"
}
