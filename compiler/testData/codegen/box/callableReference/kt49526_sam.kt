// IGNORE_BACKEND: JVM
// IGNORE_LIGHT_ANALYSIS

// IGNORE_BACKEND_FIR: JVM_IR
// FIR_STATUS: LambdaConversionException: Type mismatch for lambda argument 1: class java.lang.Object is not convertible to interface I1
// JVM_IR it this case has an approximated type 'KFun<out Any>', which has a projected top-level argument.

fun <T> intersect(x: T, y: T): T = x

interface I1
interface I2

class C1 : I1, I2 {
    override fun toString(): String = "OK"
}

class C2 : I1, I2

fun <T> T.k() = K<T>(this)

fun interface KFun<T> {
    fun invoke(x: T)
}

class K<T>(private val x: T) {
    fun with(kf: KFun<T>) {
        kf.invoke(x)
    }
}

fun box(): String {
    var result = "Failed"
    intersect(C1(), C2()).k().with { result = it.toString() }
    return result
}
