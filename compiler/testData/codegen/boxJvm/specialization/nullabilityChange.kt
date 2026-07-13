// TARGET_BACKEND: JVM
// WITH_STDLIB

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

fun <@JvmSpecialize T> makeNullable(x: T): T? = x

fun <@JvmSpecialize T> unwrap1(x: T?): T = when {
    x == null -> error("argument is in fact nullable")
    else -> return x
}

fun <@JvmSpecialize T> unwrap2(x: T?): T {
    val y = x!!
    return y
}

fun <@JvmSpecialize T> unwrap3(x: T?): T {
    val y = unwrap1(x)
    return y
}

fun <@JvmSpecialize T> unwrap4(x: T?): T {
    val y = unwrap1<T?>(x)!!
    return y
}

fun <@JvmSpecialize T> innerId(x: T) = x
fun <@JvmSpecialize T> id(x: T): T {
    return innerId<T?>(x)!!
}

@JvmInline
value class I(val x: Int)

@JvmInline
value class IN(val x: Int?)

@JvmInline
value class S(val x: String)

@JvmInline
value class SN(val x: String?)

fun box(): String {
    if (makeNullable<Int>(42) != 42) return "fail: makeNullable<Int>"
    if (makeNullable<Int?>(42) != 42) return "fail: makeNullable<Int?>"
    if (makeNullable<String>("hello") != "hello") return "fail: makeNullable<String>"
    if (makeNullable<String?>("hello") != "hello") return "fail: makeNullable<String?>"
    if (makeNullable<I>(I(42)) != I(42)) return "fail: makeNullable<I>"
    if (makeNullable<I?>(I(42)) != I(42)) return "fail: makeNullable<I?>"
    if (makeNullable<IN>(IN(42)) != IN(42)) return "fail: makeNullable<IN>"
    if (makeNullable<IN?>(IN(42)) != IN(42)) return "fail: makeNullable<IN?>"
    if (makeNullable<S>(S("hello")) != S("hello")) return "fail: makeNullable<S>"
    if (makeNullable<S?>(S("hello")) != S("hello")) return "fail: makeNullable<S?>"
    if (makeNullable<SN>(SN("hello")) != SN("hello")) return "fail: makeNullable<SN>"
    if (makeNullable<SN?>(SN("hello")) != SN("hello")) return "fail: makeNullable<SN?>"

    if (unwrap1<Int>(42) != 42) return "fail: unwrap1<Int>"
    if (unwrap1<Int?>(42) != 42) return "fail: unwrap1<Int?>"
    if (unwrap1<String>("hello") != "hello") return "fail: unwrap1<String>"
    if (unwrap1<String?>("hello") != "hello") return "fail: unwrap1<String?>"
    if (unwrap1<I>(I(42)) != I(42)) return "fail: unwrap1<I>"
    if (unwrap1<I?>(I(42)) != I(42)) return "fail: unwrap1<I?>"
    if (unwrap1<IN>(IN(42)) != IN(42)) return "fail: unwrap1<IN>"
    if (unwrap1<IN?>(IN(42)) != IN(42)) return "fail: unwrap1<IN?>"
    if (unwrap1<S>(S("hello")) != S("hello")) return "fail: unwrap1<S>"
    if (unwrap1<S?>(S("hello")) != S("hello")) return "fail: unwrap1<S?>"
    if (unwrap1<SN>(SN("hello")) != SN("hello")) return "fail: unwrap1<SN>"
    if (unwrap1<SN?>(SN("hello")) != SN("hello")) return "fail: unwrap1<SN?>"

    if (unwrap2<Int>(42) != 42) return "fail: unwrap2<Int>"
    if (unwrap2<Int?>(42) != 42) return "fail: unwrap2<Int?>"
    if (unwrap2<String>("hello") != "hello") return "fail: unwrap2<String>"
    if (unwrap2<String?>("hello") != "hello") return "fail: unwrap2<String?>"
    if (unwrap2<I>(I(42)) != I(42)) return "fail: unwrap2<I>"
    if (unwrap2<I?>(I(42)) != I(42)) return "fail: unwrap2<I?>"
    if (unwrap2<IN>(IN(42)) != IN(42)) return "fail: unwrap2<IN>"
    if (unwrap2<IN?>(IN(42)) != IN(42)) return "fail: unwrap2<IN?>"
    if (unwrap2<S>(S("hello")) != S("hello")) return "fail: unwrap2<S>"
    if (unwrap2<S?>(S("hello")) != S("hello")) return "fail: unwrap2<S?>"
    if (unwrap2<SN>(SN("hello")) != SN("hello")) return "fail: unwrap2<SN>"
    if (unwrap2<SN?>(SN("hello")) != SN("hello")) return "fail: unwrap2<SN?>"

    if (unwrap3<Int>(42) != 42) return "fail: unwrap3<Int>"
    if (unwrap3<Int?>(42) != 42) return "fail: unwrap3<Int?>"
    if (unwrap3<String>("hello") != "hello") return "fail: unwrap3<String>"
    if (unwrap3<String?>("hello") != "hello") return "fail: unwrap3<String?>"
    if (unwrap3<I>(I(42)) != I(42)) return "fail: unwrap3<I>"
    if (unwrap3<I?>(I(42)) != I(42)) return "fail: unwrap3<I?>"
    if (unwrap3<IN>(IN(42)) != IN(42)) return "fail: unwrap3<IN>"
    if (unwrap3<IN?>(IN(42)) != IN(42)) return "fail: unwrap3<IN?>"
    if (unwrap3<S>(S("hello")) != S("hello")) return "fail: unwrap3<S>"
    if (unwrap3<S?>(S("hello")) != S("hello")) return "fail: unwrap3<S?>"
    if (unwrap3<SN>(SN("hello")) != SN("hello")) return "fail: unwrap3<SN>"
    if (unwrap3<SN?>(SN("hello")) != SN("hello")) return "fail: unwrap3<SN?>"

    if (unwrap4<Int>(42) != 42) return "fail: unwrap4<Int>"
    if (unwrap4<Int?>(42) != 42) return "fail: unwrap4<Int?>"
    if (unwrap4<String>("hello") != "hello") return "fail: unwrap4<String>"
    if (unwrap4<String?>("hello") != "hello") return "fail: unwrap4<String?>"
    if (unwrap4<I>(I(42)) != I(42)) return "fail: unwrap4<I>"
    if (unwrap4<I?>(I(42)) != I(42)) return "fail: unwrap4<I?>"
    if (unwrap4<IN>(IN(42)) != IN(42)) return "fail: unwrap4<IN>"
    if (unwrap4<IN?>(IN(42)) != IN(42)) return "fail: unwrap4<IN?>"
    if (unwrap4<S>(S("hello")) != S("hello")) return "fail: unwrap4<S>"
    if (unwrap4<S?>(S("hello")) != S("hello")) return "fail: unwrap4<S?>"
    if (unwrap4<SN>(SN("hello")) != SN("hello")) return "fail: unwrap4<SN>"
    if (unwrap4<SN?>(SN("hello")) != SN("hello")) return "fail: unwrap4<SN?>"

    if (id<Int>(42) != 42) return "fail: id<Int>"
    if (id<Int?>(42) != 42) return "fail: id<Int?>"
    if (id<String>("hello") != "hello") return "fail: id<String>"
    if (id<String?>("hello") != "hello") return "fail: id<String?>"
    if (id<I>(I(42)) != I(42)) return "fail: id<I>"
    if (id<I?>(I(42)) != I(42)) return "fail: id<I?>"
    if (id<IN>(IN(42)) != IN(42)) return "fail: id<IN>"
    if (id<IN?>(IN(42)) != IN(42)) return "fail: id<IN?>"
    if (id<S>(S("hello")) != S("hello")) return "fail: id<S>"
    if (id<S?>(S("hello")) != S("hello")) return "fail: id<S?>"
    if (id<SN>(SN("hello")) != SN("hello")) return "fail: id<SN>"
    if (id<SN?>(SN("hello")) != SN("hello")) return "fail: id<SN?>"

    return "OK"
}
