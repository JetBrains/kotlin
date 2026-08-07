// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// FILE: IC.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

@JvmInline
@JvmExposeBoxed
value class Count(val value: Int)

// A nullable value class is boxed in the unboxed variant as well, so the Java caller has to be able to pass
// 'null' through the exposed one. 'Count' is primitive-backed on purpose - it is the interesting case.
@JvmExposeBoxed
fun describeId(id: Id?): String = id?.value ?: "null"

@JvmExposeBoxed
fun describeCount(count: Count?): String = count?.value?.toString() ?: "null"

// FILE: Main.java
public class Main {
    public String referenceBacked() {
        return ICKt.describeId(null);
    }

    public String primitiveBacked() {
        return ICKt.describeCount(null);
    }
}

// FILE: Box.kt
fun box(): String {
    var res = Main().referenceBacked()
    if (res != "null") return "FAIL 1: $res"
    res = Main().primitiveBacked()
    if (res != "null") return "FAIL 2: $res"
    return "OK"
}
