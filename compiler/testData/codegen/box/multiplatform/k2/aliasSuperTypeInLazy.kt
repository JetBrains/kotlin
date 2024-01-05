// LANGUAGE: +MultiPlatformProjects
// KT-64776 - test infra problems
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6, WASM
// MODULE: dep
// FILE: dep.kt

open class Base1() {
    val prop = "O"
    fun foo() = "K"
}
open class Base2 : Base1()

// MODULE: lib-common(dep)
// FILE: lib-common.kt


open expect class BaseAlias() : Base1

open class Child: BaseAlias()

// MODULE: lib-jvm(dep)()(lib-common)
// FILE: lib-jvm.kt

actual typealias BaseAlias = Base2

// MODULE: main(lib-jvm, dep)
// FILE: main.kt

class InMain : Child()

fun box() : String {
    return InMain().prop + InMain().foo()
}