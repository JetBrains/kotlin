// LANGUAGE: +MultiPlatformProjects, +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class CommonClass(sourceCommonInt: Int, sourceCommonStr: String) {
    val propCommonInt: Int
    val propCommonStr: String
}

expect open class PlatformBase(sourcePlatformInt: Int) {
    open val propOpenPlatform: Int
    protected val propProtectedPlatform: Int
    val propPlatform: Int

    fun baseProtectedValue(): Int
}

open class CommonChild(n: Int) : PlatformBase(n) {
    fun childProtectedValue(): Int {
        (val propProtectedPlatform) = this
        return propProtectedPlatform
    }
}

fun runCommonChecks(): String {
    val common = CommonClass(1, "A")

    run {
        (val propCommonStr, val propCommonInt) = common
        if (propCommonStr != "A" || propCommonInt != 1) return "FAIL"
    }

    (var propCommonInt) = common
    propCommonInt += 1
    if (propCommonInt != 2) return "FAIL"

    val commonList: List<CommonClass> = listOf(CommonClass(1, "X"), CommonClass(2, "Y"))

    run {
        val seen = mutableListOf<String>()
        for ((val index, val value) in commonList.withIndex()) {
            (val propCommonStr) = value
            seen += "[$index]=$propCommonStr"
        }
        if (seen.joinToString(", ") != "[0]=X, [1]=Y") return "FAIL"
    }

    run {
        val joined = commonList.joinToString("|") { (val propCommonStr) -> propCommonStr }
        if (joined != "X|Y") return "FAIL"
    }

    val base = PlatformBase(3)
    run {
        (val propPlatform, val propOpenPlatform) = base
        if (propPlatform != 6 || propOpenPlatform != 3) return "FAIL"
    }

    val baseProtected = base.baseProtectedValue()
    if (baseProtected != 13) return "FAIL"

    val childProtected = CommonChild(5).childProtectedValue()
    if (childProtected != 15) return "FAIL"

    return "OK"
}

// MODULE: platform()()(common)
// FILE: platform.kt

actual class CommonClass actual constructor(sourceCommonInt: Int, sourceCommonStr: String) {
    actual val propCommonInt: Int = sourceCommonInt
    actual val propCommonStr: String = sourceCommonStr
}

actual open class PlatformBase actual constructor(sourcePlatformInt: Int) {
    actual open val propOpenPlatform: Int = sourcePlatformInt
    protected actual val propProtectedPlatform: Int = sourcePlatformInt + 10
    actual val propPlatform: Int = sourcePlatformInt * 2

    actual fun baseProtectedValue(): Int {
        (val propProtectedPlatform) = this
        return propProtectedPlatform
    }

    internal val propInternalPlatform: Int = propOpenPlatform * 3
}

private fun runPlatformChecks(): String {
    val common = runCommonChecks()
    if (common != "OK") return common

    val base = PlatformBase(4)
    (val propInternalPlatform, val propOpenPlatform) = base
    if (propInternalPlatform != 12 || propOpenPlatform != 4) return "FAIL"

    return "OK"
}

fun box(): String = runPlatformChecks()
