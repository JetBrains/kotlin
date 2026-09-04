// TARGET_BACKEND: JVM
// WITH_STDLIB

// CHECK_BYTECODE_TEXT
// 0 kotlin/sequences/SequencesKt

// FILE: J.java
public class J {
    public static kotlin.jvm.functions.Function1<Integer, Boolean> predicate() { return null; }
    public static String string() { return null; }
}

// FILE: box.kt
fun box(): String {
    val predicate = J.predicate()
    val log = StringBuilder()
    try {
        sequenceOf(1, 2).map { log.append(it); it }.filter(predicate).toList()
    } catch (e: NullPointerException) {
        log.append("NPE")
    }
    val string = J.string()
    try {
        emptySequence<Int>().map(string::get).toList()
    } catch (e: NullPointerException) {
        log.append("NPE")
    }
    val r = log.toString()
    return if (r == "NPENPE") "OK" else "Fail: $r"
}
