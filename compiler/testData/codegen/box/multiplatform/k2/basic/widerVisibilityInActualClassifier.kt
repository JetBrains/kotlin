// IGNORE_BACKEND_K1: ANY
//   IGNORE_REASON: KT-59355 is fixed only in K2
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
//   IGNORE_REASON: `JsName` in js.translator/testData/_commonFiles/testUtils.kt is invisible for some reason
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-59355

// MODULE: common
// FILE: common.kt
internal expect open class Some() {
    protected class ProtectedNested() {
        fun foo(): String
    }
}

internal class SomeInheritor : Some() {
    fun callFoo(): String {
        return ProtectedNested().foo()
    }
}

internal expect open class Other() {
    fun bar(): String
}

fun commonBox(): String {
    val x = SomeInheritor().callFoo()
    val y = Other().bar()
    return x + y
}

// MODULE: platform-jvm()()(common)
// FILE: main.kt
public actual open class Some actual constructor() {
    public actual class ProtectedNested actual constructor() {
        actual fun foo(): String = "O"
    }
}

public open class PlatformOther {
    fun bar(): String = "K"
}

internal actual typealias Other = PlatformOther

fun box(): String = commonBox()

