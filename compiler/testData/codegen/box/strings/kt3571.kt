// IGNORE_BACKEND_FIR: JVM_IR
class Thing(delegate: CharSequence) : CharSequence by delegate
  
fun box(): String {
    val l = Thing("hello there").length
    return if (l == 11) "OK" else "Fail $l"
}
