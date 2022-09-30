// TARGET_BACKEND: JVM_IR
// JDK_KIND: FULL_JDK_17
// FILE: MyRec.java
public record MyRec(String name) {}

// FILE: recordPropertyAccess.kt
fun test_1(rec: MyRec) {
    rec.name
}

fun test_2(rec: MyRec) {
    rec.name()
}
