// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-63709

operator fun String.invoke(unused: String): String = "String.invoke(String)"
operator fun String.invoke(unused: Any): String = "String.invoke(Any)"
operator fun Any.invoke(unused: String): String = "Any.invoke(String)"

fun box(): String {
    var result = ""
    implicitArgumentCast().let { if (it != "OK") result += "FAIL: implicitArgumentCast(): $it\n" }
    explicitArgumentCast().let { if (it != "OK") result += "FAIL: explicitArgumentCast(): $it\n" }
    implicitReceiverCast().let { if (it != "OK") result += "FAIL: implicitReceiverCast(): $it\n" }
    explicitReceiverCast().let { if (it != "OK") result += "FAIL: explicitReceiverCast(): $it\n" }
    implicitInvokeExplicitRecieverArgumentCast().let { if (it != "OK") result += "FAIL: implicitInvokeExplicitRecieverArgumentCast(): $it\n" }
    explicitInvokeImplicitRecieverArgumentCast().let { if (it != "OK") result += "FAIL: explicitInvokeImplicitRecieverArgumentCast(): $it\n" }
    explicitInvokeExplicitRecieverArgumentCast().let { if (it != "OK") result += "FAIL: explicitInvokeExplicitRecieverArgumentCast(): $it\n" }

    return if (result.length == 0) "OK" else result
}

fun implicitArgumentCast(): String {
    val a: Any = ""
    return when (val result = a(a as String)) {
        "Any.invoke(String)" -> "OK"
        else -> result
    }
}

fun explicitArgumentCast(): String {
    val a: Any = ""
    return when (val result = a.invoke(a as String)) {
        "Any.invoke(String)" -> "OK"
        else -> result
    }
}

fun implicitReceiverCast(): String {
    val a: Any = ""
    return when (val result = (a as String)(a)) {
        "String.invoke(String)" -> "OK" // Unique to K2
        else -> result
    }
}


fun explicitReceiverCast(): String {
    val a: Any = ""
    return when (val result = (a as String).invoke(a)) {
        "String.invoke(String)" -> "OK"
        else -> result
    }
}

fun implicitInvokeExplicitRecieverArgumentCast(): String {
    val a: Any = ""
    with (a) {
        return when (val result = this(this as String)) {
            "Any.invoke(String)" -> "OK"
            else -> result
        }
    }
}

fun explicitInvokeImplicitRecieverArgumentCast(): String {
    val a: Any = ""
    with (a) {
        return when (val result = invoke(this as String)) {
            "Any.invoke(String)" -> "OK"
            else -> result
        }
    }
}

fun explicitInvokeExplicitRecieverArgumentCast(): String {
    val a: Any = ""
    with (a) {
        return when (val result = this.invoke(this as String)) {
            "Any.invoke(String)" -> "OK"
            else -> result
        }
    }
}
