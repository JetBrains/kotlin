// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib1-common
expect open class Lib1A()

fun <T> useGeneric1(t: T): String where T : Lib1A = "lib1Generic"

// MODULE: lib1-inter()()(lib1-common)
fun <T> useGeneric1Inter(t: T): String where T : Lib1A = "lib1GenericInter"

// MODULE: lib1-platform()()(lib1-inter)
actual open class Lib1A actual constructor()


// MODULE: lib2-common
expect open class Lib2B()

fun <T> useGeneric2(t: T): String where T : Lib2B = "lib2Generic"

// MODULE: lib2-inter()()(lib2-common)
fun <T> useGeneric2Inter(t: T): String where T : Lib2B = "lib2GenericInter"

// MODULE: lib2-platform()()(lib2-inter)
actual open class Lib2B actual constructor()


// MODULE: app-common(lib1-common, lib2-common)
class AppA1 : Lib1A()
class AppB1 : Lib2B()

fun appCommonUse(a: AppA1, b: AppB1): String = useGeneric1(a) + useGeneric2(b)

// MODULE: app-inter(lib1-inter, lib2-inter)(lib1-common, lib2-common)(app-common)
fun appInterUse(a: AppA1, b: AppB1): String {
    val fromCommon = appCommonUse(a, b)
    val fromInter1 = useGeneric1Inter(a)
    val fromInter2 = useGeneric2Inter(b)
    return "$fromCommon$fromInter1$fromInter2"
}

// MODULE: app-platform(lib1-platform, lib2-platform)()(app-inter)
fun box(): String {
    val a = AppA1()
    val b = AppB1()
    val r = appInterUse(a, b)
    val exp = "lib1Genericlib2Genericlib1GenericInterlib2GenericInter"
    return if (r == exp) "OK" else "FAIL"
}
