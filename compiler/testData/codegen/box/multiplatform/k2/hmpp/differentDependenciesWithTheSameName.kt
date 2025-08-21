// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-80064

// MODULE: lib-common
typealias Some = Int

// MODULE: lib-inter()()(lib-common)

// MODULE: lib-platform()()(lib-inter)

// MODULE: lib2-common

// MODULE: lib2-inter()()(lib2-common)
typealias Some = Int

// MODULE: lib2-platform()()(lib2-inter)

// MODULE: app-common(lib-common)
fun testAppCommon(c: Some) {}

// MODULE: app-inter(lib2-inter)()(app-common)
fun testAppIntermediate(s: Some) {}

// MODULE: app-platform(lib-platform, lib2-platform)()(app-inter)
fun testAppPlatform(s: Some) {}

fun box() = "OK"