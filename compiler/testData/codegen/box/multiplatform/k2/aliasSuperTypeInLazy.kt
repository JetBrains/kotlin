// LANGUAGE: +MultiPlatformProjects
// MODULE: dep-common
// FILE: dep.kt
open class Base1() {
    val prop = "O"
    fun foo() = "K"
}
open class Base2 : Base1()

// MODULE: dep()()(dep-common)

// MODULE: lib-common(dep-common)
// FILE: lib-common.kt

open expect class BaseAlias() : Base1

open class Child: BaseAlias()

// MODULE: lib(dep)()(lib-common)
// FILE: lib.kt

actual typealias BaseAlias = Base2

// MODULE: main(lib, dep)
// FILE: main.kt

class InMain : Child()

fun box() : String {
    return InMain().prop + InMain().foo()
}
