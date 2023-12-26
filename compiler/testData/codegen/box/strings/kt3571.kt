// JVM_ABI_K1_K2_DIFF: KT-63828

class Thing(delegate: CharSequence) : CharSequence by delegate
  
fun box(): String {
    val l = Thing("hello there").length
    return if (l == 11) "OK" else "Fail $l"
}
