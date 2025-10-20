// LANGUAGE: +MultiPlatformProjects, +NameBasedDestructuring
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
        (val extractedProtected: Int = propProtectedPlatform) = this
        return extractedProtected
    }
}

fun runCommonChecks(): String {
    val common = CommonClass(1, "A")

    run {
        (val extractedCommonInt: Int = propCommonInt, val extractedCommonStr: String = propCommonStr) = common
        if (extractedCommonInt != 1 || extractedCommonStr != "A") return "FAIL"
    }

    (var commonIntVar: Int = propCommonInt) = common
    commonIntVar += 1
    if (commonIntVar != 2) return "FAIL"

    val commonList: List<CommonClass> = listOf(CommonClass(1, "X"), CommonClass(2, "Y"))

    run {
        var seen = mutableListOf<String>()
        for ((val idx: Int = index, val elem: CommonClass = value) in commonList.withIndex()) {
            (val s: String = propCommonStr) = elem
            seen += "[$idx]=$s"
        }
        if (seen.joinToString(", ") != "[0]=X, [1]=Y") return "FAIL"
    }

    run {
        val joined = commonList.joinToString("|") { (val s: String = propCommonStr) -> s }
        if (joined != "X|Y") return "FAIL"
    }

    val base = PlatformBase(3)
    run {
        (val extractedOpen: Int = propOpenPlatform, val extractedPublic: Int = propPlatform) = base
        if (extractedOpen != 3 || extractedPublic != 6) return "FAIL"
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
        (val extractedProtected: Int = propProtectedPlatform) = this
        return extractedProtected
    }

    internal val propInternalPlatform: Int = propOpenPlatform * 3
}

private fun runPlatformChecks(): String {
    val common = runCommonChecks()
    if (common != "OK") return common

    val base = PlatformBase(4)
    (val extractedOpen: Int = propOpenPlatform, val extractedInternal: Int = propInternalPlatform) = base
    if (extractedOpen != 4 || extractedInternal != 12) return "FAIL"

    return "OK"
}

fun box(): String = runPlatformChecks()
