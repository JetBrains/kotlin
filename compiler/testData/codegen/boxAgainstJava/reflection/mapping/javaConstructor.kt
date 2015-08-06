import kotlin.reflect.*
import kotlin.reflect.jvm.*
import javaConstructor as J

fun box(): String {
    val reference = ::J
    val javaConstructor = reference.javaConstructor ?: return "Fail: no Constructor for reference"
    val j = javaConstructor.newInstance("OK")
    val kotlinConstructor = javaConstructor.kotlinFunction
    if (reference != kotlinConstructor) return "Fail: reference != kotlinConstructor"
    return j.result
}
