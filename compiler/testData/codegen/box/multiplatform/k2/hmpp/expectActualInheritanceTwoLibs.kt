// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib1-common
expect open class Lib1A() {
    open fun fromLib1(): String
}

// MODULE: lib1-inter()()(lib1-common)
fun Lib1A.fromLib1Inter(): String = "lib1Inter${fromLib1()}"

// MODULE: lib1-platform()()(lib1-inter)
actual open class Lib1A actual constructor() {
    actual open fun fromLib1(): String = "lib1Platform"
}

// MODULE: lib2-common
expect open class Lib2B() {
    open fun fromLib2(): String
}

// MODULE: lib2-inter()()(lib2-common)
fun Lib2B.fromLib2Inter(): String = "lib2Inter${fromLib2()}"

// MODULE: lib2-platform()()(lib2-inter)
actual open class Lib2B actual constructor() {
    actual open fun fromLib2(): String = "lib2Platform"
}

// MODULE: app-common(lib1-common, lib2-common)
open class AppA : Lib1A() {
    override fun fromLib1(): String = "app1"
    fun fromApp1(): String = "appOnly1"
}

open class AppB : Lib2B() {
    override fun fromLib2(): String = "app2"
    fun fromApp2(): String = "appOnly2"
}

fun useCommon(a: Lib1A, b: Lib2B): String = a.fromLib1() + b.fromLib2()

// MODULE: app-inter(lib1-inter, lib2-inter)(lib1-common, lib2-common)(app-common)
fun useInter(a: Lib1A, b: Lib2B): String {
    val fromCommon = useCommon(a, b)
    val fromInter1 = a.fromLib1Inter()
    val fromInter2 = b.fromLib2Inter()
    return "$fromCommon$fromInter1$fromInter2"
}

// MODULE: app-platform(lib1-platform, lib2-platform)()(app-inter)
fun usePlatform(a: Lib1A, b: Lib2B): String {
    val fromInter = useInter(a, b)
    val isAppA = a is AppA
    val app1Part = if (a is AppA) a.fromApp1() else "noApp1"
    val isAppB = b is AppB
    val app2Part = if (b is AppB) b.fromApp2() else "noApp2"

    return "$fromInter$isAppA$app1Part$isAppB$app2Part"
}

fun box(): String {
    val a: Lib1A = AppA()
    val b: Lib2B = AppB()

    val r = usePlatform(a, b)
    val exp = "app1app2lib1Interapp1lib2Interapp2trueappOnly1trueappOnly2"
    return if (r == exp) "OK" else "FAIL"
}
