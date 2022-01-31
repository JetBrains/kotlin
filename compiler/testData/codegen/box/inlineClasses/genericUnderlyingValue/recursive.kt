// LANGUAGE: -JvmInlineValueClasses, +GenericInlineClassParameter
// IGNORE_BACKED: JVM

inline class ICAny<T>(val value: T)

fun box(): String {
    var res = ICAny("OK").value
    if (res != "OK") return "FAIL 1: $res"
    res = ICAny(ICAny("OK")).value.value
    if (res != "OK") return "FAIL 2: $res"
    res = ICAny(ICAny(ICAny("OK"))).value.value.value
    if (res != "OK") return "FAIL 3: $res"
    return "OK"
}