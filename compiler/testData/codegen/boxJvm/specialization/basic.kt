// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 1 INVOKESTATIC java/lang/Integer.valueOf
// 1 INVOKESTATIC java/lang/Long.valueOf
// 1 INVOKESTATIC java/lang/Double.valueOf
// 1 INVOKESTATIC I.box-impl
// 1 INVOKESTATIC D.box-impl
// 1 INVOKESTATIC S.box-impl
// 1 INVOKEDYNAMIC id\(Ljava/lang/Object;\)Ljava/lang/Object;
// 2 INVOKEDYNAMIC id\(I\)I
// 1 INVOKEDYNAMIC id\(J\)J
// 2 INVOKEDYNAMIC id\(D\)D
// 1 INVOKEDYNAMIC id\(Ljava/lang/String;\)Ljava/lang/String;
// 1 INVOKEDYNAMIC makeHash\(LC;I\)Ljava/lang/String;

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

@JvmInline
value class I(val value: Int)

@JvmInline
value class D(val value: Double)

@JvmInline
value class S(val value: String)

class C(val prompt: String) {
    fun <@JvmSpecialize T> makeHash(x: T) = "$prompt: ${x.hashCode()}"
}

fun <@JvmSpecialize T> id(x: T) = x

fun <T> boxId(x: T) = x

fun box(): String {
    if (boxId("abc") != "abc" ||
            boxId(42) != 42 ||
            boxId(42L) != 42L ||
            boxId(42.0) != 42.0 ||
            boxId(I(42)) != I(42) ||
            boxId(D(42.0)) != D(42.0) ||
            boxId(S("abc")) != S("abc")
    ) {
        return "unexpected: boxId fails"
    }

    if (id("abc") != "abc") return "fail: for string"
    if (id(42) != 42) return "fail: for int"
    if (id(42L) != 42L) return "fail: for long"
    if (id(42.0) != 42.0) return "fail: for double"
    if (id(I(42)) != I(42)) return "fail: for inline backed by int"
    if (id(D(42.0)) != D(42.0)) return "fail: for inline backed by double"
    if (id(S("abc")) != S("abc")) return "fail: for inline backed by string"

    if (!C("hash is").makeHash(42).startsWith("hash is: ")) return "fail: for member method"

    return "OK"
}
