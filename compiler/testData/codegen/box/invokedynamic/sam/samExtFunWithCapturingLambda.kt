// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// SAM_CONVERSIONS: INDY

fun interface IExtFun {
    fun String.foo(s: String) : String
}

fun box(): String {
    val oChar = 'O'
    val iExtFun = IExtFun { this + oChar.toString() + it }
    return with(iExtFun) {
        "".foo("K")
    }
}
