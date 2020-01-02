fun less1(a: Double, b: Double) = a.compareTo(b) == -1

fun less2(a: Double?, b: Double?) = a!!.compareTo(b!!) == -1

fun less3(a: Double?, b: Double?) = a != null && b != null && a.compareTo(b) == -1

fun less4(a: Double?, b: Double?) = if (a is Double && b is Double) a.compareTo(b) == -1 else null!!

fun less5(a: Any?, b: Any?) = if (a is Double && b is Double) a.compareTo(b) == -1 else null!!

fun box(): String {
    if (!less1(-0.0, 0.0)) return "fail 1"
    if (!less2(-0.0, 0.0)) return "fail 2"
    if (!less3(-0.0, 0.0)) return "fail 3"
    if (!less4(-0.0, 0.0)) return "fail 4"
    if (!less5(-0.0, 0.0)) return "fail 5"

    return "OK"
}