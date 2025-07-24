// JVM_ABI_K1_K2_DIFF: Delegation to stdlib class annotated with @MustUseReturnValue (KT-79125)
class Thing(delegate: CharSequence) : CharSequence by delegate
  
fun box(): String {
    val l = Thing("hello there").length
    return if (l == 11) "OK" else "Fail $l"
}
