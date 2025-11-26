// TARGET_BACKEND: JS_IR, JS_IR_ES6
// LANGUAGE: +MultiPlatformProjects
// MODULE: lib-common
// FILE: lib-common.kt
expect open class PotentiallyRegularClass() {
    fun foo(): String
}

expect open class PotentiallyExternalClass() {
    fun bar(): String
}

class Parent {
    class FirstNested : PotentiallyRegularClass()
    class SecondNested : PotentiallyExternalClass()
}

// MODULE: lib-platform()()(lib-common)
// FILE: lib-platform.kt
actual open class PotentiallyRegularClass {
    actual fun foo(): String = "Just A Regular Class"
}

actual external open class PotentiallyExternalClass {
    actual fun bar(): String
}

// FILE: PotentiallyExternalClass.js
function PotentiallyExternalClass() {}

PotentiallyExternalClass.prototype.bar = function () {
    return "External Class";
};

// MODULE: app-platform(lib-platform)()()
// FILE: app-platform.kt
fun box(): String {
    val firstNested = Parent.FirstNested()
    if (firstNested.foo() != "Just A Regular Class") return "Fail: foo is called wrongly"

    val secondNested = Parent.SecondNested()
    if (secondNested.bar() != "External Class") return "Fail: bar is called wrongly"

    return "OK"
}
