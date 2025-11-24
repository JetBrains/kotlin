// LANGUAGE: +MultiPlatformProjects +ExpectRefinement
// WITH_STDLIB

// MODULE: lib-common
expect open class LibClass() {
    open fun foo(): String
}

// MODULE: lib-inter()()(lib-common)
@OptIn(kotlin.ExperimentalMultiplatform::class)
@kotlin.experimental.ExpectRefinement
expect open class LibClass() {
    open fun foo(): String
    open fun bar(): String
}

// MODULE: lib-platform()()(lib-inter)
actual open class LibClass {
    actual constructor()

    actual open fun foo(): String = "foo-lib-platform"
    actual open fun bar(): String = "bar-lib-platform"
}

// MODULE: app-common(lib-common)
class AppCommon : LibClass() {
    override fun foo(): String = "foo-app-common"
}

// MODULE: app-inter(lib-inter)()(app-common)
class AppInter : LibClass() {
    override fun foo(): String = "foo-app-inter"
    override fun bar(): String = "bar-app-inter"
}

// MODULE: app-platform(lib-platform)()(app-inter)
class AppPlatform : LibClass() {
    override fun foo(): String = "foo-app-platform"
    override fun bar(): String = "bar-app-platform"
}

fun box(): String {
    if (LibClass().foo() != "foo-lib-platform") return "FAIL"
    if (LibClass().bar() != "bar-lib-platform") return "FAIL"
    if (AppCommon().foo() != "foo-app-common") return "FAIL"
    if (AppInter().bar() != "bar-app-inter") return "FAIL"
    if (AppPlatform().foo() != "foo-app-platform") return "FAIL"
    return "OK"
}
