// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
fun box(): String {
    if (0.toByte().compareTo(-0.0) != 1) return "fail 1.1"
    if (0.toByte().compareTo(-0.0F) != 1) return "fail 1.2"
    if (0.toByte().compareTo(Double.NaN) != -1) return "fail 1.3"
    if (0.toByte().compareTo(Float.NaN) != -1) return "fail 1.4"

    if (0.toShort().compareTo(-0.0) != 1) return "fail 2.1"
    if (0.toShort().compareTo(-0.0F) != 1) return "fail 2.2"
    if (0.toShort().compareTo(Double.NaN) != -1) return "fail 2.3"
    if (0.toShort().compareTo(Float.NaN) != -1) return "fail 2.4"

    if (0.compareTo(-0.0) != 1) return "fail 3.1"
    if (0.compareTo(-0.0F) != 1) return "fail 3.2"
    if (0.compareTo(Double.NaN) != -1) return "fail 3.3"
    if (0.compareTo(Float.NaN) != -1) return "fail 3.4"

    if (0.0F.compareTo(-0.0) != 1) return "fail 4.1"
    if (0.0F.compareTo(-0.0F) != 1) return "fail 4.2"
    if (0.0F.compareTo(Double.NaN) != -1) return "fail 4.3"
    if (0.0F.compareTo(Float.NaN) != -1) return "fail 4.4"

    if (0.0.compareTo(-0.0) != 1) return "fail 5.1"
    if (0.0.compareTo(-0.0F) != 1) return "fail 5.2"
    if (0.0.compareTo(Double.NaN) != -1) return "fail 5.3"
    if (0.0.compareTo(Float.NaN) != -1) return "fail 5.4"

    if (0L.compareTo(-0.0) != 1) return "fail 6.1"
    if (0L.compareTo(-0.0F) != 1) return "fail 6.2"
    if (0L.compareTo(Double.NaN) != -1) return "fail 6.3"
    if (0L.compareTo(Float.NaN) != -1) return "fail 6.4"


    if ((-0.0).compareTo(0.toByte()) != -1) return "fail 7.1"
    if ((-0.0).compareTo(0.toShort()) != -1) return "fail 7.2"
    if ((-0.0).compareTo(0) != -1) return "fail 7.3"
    if ((-0.0).compareTo(0.0F) != -1) return "fail 7.4"
    if ((-0.0).compareTo(0.0) != -1) return "fail 7.5"
    if ((-0.0).compareTo(0L) != -1) return "fail 7.6"

    if ((-0.0F).compareTo(0.toByte()) != -1) return "fail 8.1"
    if ((-0.0F).compareTo(0.toShort()) != -1) return "fail 8.2"
    if ((-0.0F).compareTo(0) != -1) return "fail 8.3"
    if ((-0.0F).compareTo(0.0F) != -1) return "fail 8.4"
    if ((-0.0F).compareTo(0.0) != -1) return "fail 8.5"
    if ((-0.0F).compareTo(0L) != -1) return "fail 8.6"

    if (Double.NaN.compareTo(0.toByte()) != 1) return "fail 9.1"
    if (Double.NaN.compareTo(0.toShort()) != 1) return "fail 9.2"
    if (Double.NaN.compareTo(0) != 1) return "fail 9.3"
    if (Double.NaN.compareTo(0.0F) != 1) return "fail 9.4"
    if (Double.NaN.compareTo(0.0) != 1) return "fail 9.5"
    if (Double.NaN.compareTo(0L) != 1) return "fail 9.6"

    if (Float.NaN.compareTo(0.toByte()) != 1) return "fail 10.1"
    if (Float.NaN.compareTo(0.toShort()) != 1) return "fail 10.2"
    if (Float.NaN.compareTo(0) != 1) return "fail 10.3"
    if (Float.NaN.compareTo(0.0F) != 1) return "fail 10.4"
    if (Float.NaN.compareTo(0.0) != 1) return "fail 10.5"
    if (Float.NaN.compareTo(0L) != 1) return "fail 10.6"

    return "OK"
}