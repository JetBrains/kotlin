// !API_VERSION: 1.5
// !LANGUAGE: +JvmRecordSupport
// SKIP_TXT
// JVM_TARGET: 15
// ENABLE_JVM_PREVIEW

@JvmRecord
class A0

@JvmRecord
class A1 {
    constructor()
}

@JvmRecord
class A2()

@JvmRecord
class A3(name: String)

@JvmRecord
class A4(var name: String)

@JvmRecord
class A5(vararg val name: String, y: Int)

@JvmRecord
open class A6(val x: String)

@JvmRecord
abstract class A7(val x: String)

@JvmRecord
sealed class A8(val x: String)

@JvmRecord
enum class A9(val x: String) {
    X("");
}

@JvmRecord
class A10(
    val x: String,
    val y: Int,
    vararg val z: Double,
)

fun main() {
    @JvmRecord
    class Local
}

class Outer {
    @JvmRecord
    inner class Inner(val name: String)
}
