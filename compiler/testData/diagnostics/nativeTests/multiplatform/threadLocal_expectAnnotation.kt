// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
import kotlin.reflect.KProperty

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyThreadLocal<!>()

class Delegate {
    val value: Int = 10
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

class AtomicInt(var value: Int)
object Foo {
    var field1: Int = 10
    val backer2 = AtomicInt(0)
    var field2: Int
        get() = backer2.value
        set(value: Int) { backer2.value = value }
}

object Foo1 {
    var field1: Int = 10
        set(value: Int) { backer2.value = value }
    val backer2 = AtomicInt(0)
}

object WithDelegate {
    var field1: Int by Delegate()
}

@MyThreadLocal
object Bar {
    var field1: Int = 10
    var field2: String? = null
}

class Foo2 {
    companion object {
        var field1: Int = 10
        val backer2 = AtomicInt(0)
        var field2: Int
            get() = backer2.value
            set(value: Int) {
                backer2.value = value
            }
    }
}

class Bar2 {
    @MyThreadLocal
    companion object {
        var field1: Int = 10
        var field2: String? = null
    }
}

<!INAPPLICABLE_THREAD_LOCAL{NATIVE}!>@MyThreadLocal<!>
enum class Color(var rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF)
}

enum class Color1(var rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF);

    init { this.rgb += 1 }
}

@MyThreadLocal
var a = 3
enum class Color2() {
    RED(),
    GREEN(),
    BLUE();

    var rgb: Int = 2
        set(value: Int) {
            a = value
        }
}

enum class Color3() {
    RED(),
    GREEN(),
    BLUE();

    var field1: Int by Delegate()
}

enum class Color4 {
    RED {
        var a = 2
        override fun foo() { a = 42 }
    },
    GREEN,
    BLUE;
    open fun foo() {}
}

var topLevelProperty = "Global var"

// MODULE: platform()()(common)
// FILE: platform.kt

actual typealias MyThreadLocal = kotlin.native.concurrent.ThreadLocal
