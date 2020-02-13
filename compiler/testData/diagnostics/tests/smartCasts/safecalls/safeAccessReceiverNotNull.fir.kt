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
    s.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
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
        a.<!UNRESOLVED_REFERENCE!>extra<!>.<!UNRESOLVED_REFERENCE!>hashCode<!>()
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
    (a?.let { useA(a) } ?: a.<!INAPPLICABLE_CANDIDATE!>y<!> ).toString()
}

fun String.correct() = true

fun kt8492(s: String?) {
    if (s?.correct() ?: false) {
        // To be supported
        s.<!INAPPLICABLE_CANDIDATE!>length<!>
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
        w.unwrap().<!INAPPLICABLE_CANDIDATE!>length<!>
    }
}

class Invokable(val x: String) {
    operator fun invoke() = x
}

class InvokableProperty(val i: Invokable)

fun checkInvokable(ip: InvokableProperty?) {
    if (ip?.i() == "Hello") {
        ip.<!INAPPLICABLE_CANDIDATE!>hashCode<!>()
    }
}