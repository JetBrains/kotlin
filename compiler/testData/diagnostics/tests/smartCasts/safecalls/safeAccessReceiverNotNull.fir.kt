// !LANGUAGE: -SafeCastCheckBoundSmartCasts -BooleanElvisBoundSmartCasts
// A set of examples for
// "If the result of a safe call is not null, understand that its receiver is not null"
// and some other improvements for nullability detection

fun kt6840_1(s: String?) {
    val hash = s?.hashCode()
    if (hash != null) {
        s.length
    }
}

fun kt6840_2(s: String?) {
    if (s?.hashCode() != null) {
        s.length
    }
}

fun kt1635(s: String?) {
    s?.hashCode()!!
    s.hashCode()
}

fun kt2127() {
    val s: String? = ""
    if (s?.length != null) {
        s.hashCode()
    }
}

fun kt3356(s: String?): Int {
    if (s?.length != 1) return 0
    return s.length
}

open class SomeClass(val data: Any)

class SubClass(val extra: Any, data: Any) : SomeClass(data)

fun kt4565_1(a: SomeClass?) {
    val data = a?.data
    if (data != null) {
        data.hashCode()
        a.hashCode()
        a.data.hashCode()
    }
    if (a?.data != null) {
        // To be supported (?!)
        data.hashCode()
        a.hashCode()
        a.data.hashCode()
    }
    if (a?.data is String) {
        a.data.length
        data.length
    }
}

fun kt4565_2(a: SomeClass?) {
    // To be supported
    if (a as? SubClass != null) {
        a.extra.hashCode()
    }
    val extra = (a as? SubClass)?.extra
    if (extra != null) {
        a.extra.hashCode()
    }
}

inline fun <reified T : SomeClass> kt45345(a: SomeClass?) {
    if (a?.data is T) {
        a.data
    }
}

inline fun <reified T : U, U : SomeClass> kt45345_2(a: SomeClass?) {
    if (a?.data is T) {
        a.data
    }
}

inline fun <reified T : U, U : SomeClass?> kt45345_3(a: SomeClass?) {
    if (a?.data is T) {
        a<!UNSAFE_CALL!>.<!>data
    }
}

class A(val y: Int)

fun kt7491_1() {
    val x: A? = A(42)
    val z = x?.y ?: return
    x.y
}

fun getA(): A? = null
fun useA(a: A): Int = a.hashCode()

fun kt7491_2() {
    val a = getA()
    (a?.let { useA(a) } ?: a<!UNSAFE_CALL!>.<!>y ).toString()
}

fun String.correct() = true

fun kt8492(s: String?) {
    if (s?.correct() ?: false) {
        // To be supported
        s<!UNSAFE_CALL!>.<!>length
    }
}

fun kt11085(prop: String?) {
    when (prop?.hashCode()) {
        1 -> prop.length
    }
}

class HttpExchange(val code: String)

fun kt11313(arg: HttpExchange?) {
    when (arg?.code) {
        "GET" -> handleGet(arg)
        "POST" -> handlePost(arg)
    }
}

fun handleGet(arg: HttpExchange) = arg

fun handlePost(arg: HttpExchange) = arg

class Wrapper {
    fun unwrap(): String? = "Something not consistent"
}

fun falsePositive(w: Wrapper) {
    if (w.unwrap() != null) {
        // Here we should NOT have smart cast
        w.unwrap()<!UNSAFE_CALL!>.<!>length
    }
}

class Invokable(val x: String) {
    operator fun invoke() = x
}

class InvokableProperty(val i: Invokable)

fun checkInvokable(ip: InvokableProperty?) {
    if (ip?.i() == "Hello") {
        ip.hashCode()
    }
}
