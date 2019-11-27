// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions +SamConversionPerArgument
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: Fn.java
public interface Fn<T, R> {
    R run(String s, int i, T t);
}

// FILE: 1.kt
class K {
    fun runConversion(f1: Fn<String, Int>, f2: Fn<Int, String>) = f1.run("Bar", 1, f2.run("Foo", 42, 239))
}

fun box(): String {
    val k = K()
    var x = ""

    val fsi = object : Fn<String, Int> {
        override fun run(s: String, i: Int, t: String): Int {
            x += "$s$i$t "
            return i
        }
    }

    val fis = object : Fn<Int, String> {
        override fun run(s: String, i: Int, t: Int): String {
            x += "$s$i$t "
            return s
        }
    }

    val r1 = k.runConversion(fsi) { s, i, ti -> x += "L$s$i$ti "; "L$s"}
    val r2 = k.runConversion({ s, i, ts -> x += "L$s$i$ts"; i }, fis)

    if (r1 != 1) return "fail r1: $r1"
    if (r2 != 1) return "fail r2: $r2"

    if (x != "LFoo42239 Bar1LFoo Foo42239 LBar1Foo") return x

    return "OK"
}