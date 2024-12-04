// FIR_IDENTICAL
// FILE: annotation.kt
package kotlin.native.concurrent

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ThreadLocal

// FILE: test.kt
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty

data class Point(val x: Double, val y: Double)

class Person(val name: String) {
    <!INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL!>@ThreadLocal<!>
    var surname: String? = null
}

abstract class Information {
    abstract var field: String
}

<!INAPPLICABLE_THREAD_LOCAL!>@ThreadLocal<!>
class Person1(val name: String) {
    var surname: String? = null
    <!INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL!>@ThreadLocal<!>
    val extraInfo: Information = object : Information() {
        override var field: String = "extra info"
    }
}

@ThreadLocal
val extraInfo: Information = object : Information() {
    override var field: String = "extra info"
}

@ThreadLocal
val point1 = Point(1.0, 1.0)

<!INAPPLICABLE_THREAD_LOCAL!>@ThreadLocal<!>
val cornerPoint: Point
    get() = point1

@ThreadLocal
val person = Person1("aaaaa")

class Delegate {
    val value: Int = 10
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

@ThreadLocal
var field1: Int by Delegate()

@ThreadLocal
object WithDelegate {
    var field1: Int by Delegate()
}

class Bar {
    @ThreadLocal
    object SomeObject {
        var field1: Int = 10
        var field2: String? = null
    }
}