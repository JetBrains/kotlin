// TARGET_BACKEND: JVM_IR
// JDK_KIND: FULL_JDK_17
// WITH_STDLIB
// JVM_TARGET: 17

@JvmRecord
data class MyRec(val name: String)

fun test(rec: MyRec) {
    rec.name
}
