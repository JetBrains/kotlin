// TARGET_BACKEND: JVM
// JDK_KIND: FULL_JDK_17
// DUMP_IR_DIFFERENCE: JKLIB
// ^ JKlib reads Java declarations from compiled classes, not from sources.
// FILE: MyRec.java
public record MyRec(String name) {}

// FILE: recordPropertyAccess.kt
fun test_1(rec: MyRec) {
    rec.name
}

fun test_2(rec: MyRec) {
    rec.name()
}
