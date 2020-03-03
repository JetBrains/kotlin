// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
import java.util.Hashtable

class A : Hashtable<String, String>()

fun box(): String {
    val sz = A().size
    return "OK"
}