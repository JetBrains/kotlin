// WITH_STDLIB
// SKIP_KT_DUMP
// DUMP_IR_DIFFERENCE: JVM
// ^ Expected type inference difference, due to different inheritance chains for kotlin.Char and kotlin.String:
//   JVM: Char and String are derived from both interfaces
//        - java.io.Serializable (not via source of K/JVM stdlib, but injected as an additional supertype by FirJvmDeserializationExtension.addSerializableIfNeeded)
//        - kotlin.Comparable.
//        Hence, their type intersection are approximated to `Any` as the closest common supertype
//   Non-JVM: Char and String are derived from kotlin.Comparable only, so their type intersection are approximated as `Comparable<Char & String>`,
//            which is Comparable<Nothing>

fun test(): Boolean {
    val ref = (listOf('a') + "-")::contains
    return ref('a')
}
