// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: lib1-common
expect class Lib1Box {
    val v: String
}

inline fun <T> Lib1Box.map1(block: (String) -> T): T = block(v)

// MODULE: lib1-inter()()(lib1-common)
inline fun Lib1Box.interMap1(): String = this.map1 { "lib1Inter$it" }

// MODULE: lib1-platform()()(lib1-inter)
actual class Lib1Box constructor(
    actual val v: String
)

// MODULE: lib2-common
expect class Lib2Box {
    val v: String
}

inline fun <T> Lib2Box.map2(block: (String) -> T): T = block(v)

// MODULE: lib2-inter()()(lib2-common)
inline fun Lib2Box.interMap2(): String = this.map2 { "lib2Inter$it" }

// MODULE: lib2-platform()()(lib2-inter)
actual class Lib2Box constructor(
    actual val v: String
)

// MODULE: app-common(lib1-common, lib2-common)
typealias AppBox1 = Lib1Box
typealias AppBox2 = Lib2Box

inline fun AppBox1.appMap1(block: (String) -> String): String =
    this.map1(block) + "app1"

inline fun AppBox2.appMap2(block: (String) -> String): String =
    this.map2(block) + "app2"

// MODULE: app-inter(lib1-inter, lib2-inter)(lib1-common, lib2-common)(app-common)
fun appInterUse(b1: AppBox1, b2: AppBox2): String {
    val fromInter1 = b1.interMap1()
    val fromInter2 = b2.interMap2()
    val fromApp1 = b1.appMap1 { it }
    val fromApp2 = b2.appMap2 { it }
    return "$fromInter1$fromApp1$fromInter2$fromApp2"
}

// MODULE: app-platform(lib1-platform, lib2-platform)()(app-inter)
fun box(): String {
    val b1 = Lib1Box("OK1")
    val b2 = Lib2Box("OK2")
    val r = appInterUse(b1, b2)
    val exp = "lib1InterOK1OK1app1lib2InterOK2OK2app2"
    return if (r == exp) "OK" else "FAIL"
}
