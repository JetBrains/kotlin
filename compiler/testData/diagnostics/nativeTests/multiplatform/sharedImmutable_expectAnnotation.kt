// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -UNUSED_PARAMETER -DEPRECATION
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
import kotlin.reflect.KProperty

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MySharedImmutable<!>()

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyThreadLocal<!>()

fun println(value: Int) {}
fun println(value: String) {}
fun println(value: Point) {}

data class Point(val x: Double, val y: Double)
@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!>
val point1 = Point(1.0, 1.0)

<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
var point2 = Point(2.0, 2.0)

class Date(<!INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!> val month: Int, <!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}, INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!> var day:Int)
class Person(val name: String) {
    <!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}, INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
    var surname: String? = null
}

class Figure {
    <!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}, INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
    val cornerPoint: Point
        get() = point1
}

<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
var age = 20
    get() {
        println("Age is: $field")
        return field
    }
    set(value) {
        println(value)
    }

var globalAge = 30
<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
var age1 = 20
    get() {
        println("Age is: $field")
        return field
    }
    set(value) {
        globalAge = value
    }

@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!>
val age2 = 20
    get() {
        println("Age is: $field")
        return field
    }

<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
var point3: Point
    get() = point2
    set(value) {
        point2 = value
    }

<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
var point4: Point
    get() = point2
    set(value) {
        println(value)
    }

@MyThreadLocal
var point0 = Point(2.0, 2.0)

<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
var point5: Point
    get() = point0
    set(value) {
        point0 = value
    }


class Delegate {
    var value = 20
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        println("Get")
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        println("Set")
    }
}

@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!>
var property: Int by Delegate()

class Delegate1 {
    var value = 20
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        this.value = value
    }
}

@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!>
var property1: Int by Delegate1()

var globalValue: Int = 20

class Delegate2 {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return globalValue
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        println(value)
    }
}

@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!>
var property2: Int by Delegate2()

<!INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY{NATIVE}!>@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!><!>
val someValue: Int
    get() = 20

@<!TYPEALIAS_EXPANSION_DEPRECATION{NATIVE}!>MySharedImmutable<!>
val someValueWithDelegate by Delegate()

// MODULE: platform()()(common)
// FILE: platform.kt

actual typealias MySharedImmutable = kotlin.native.concurrent.SharedImmutable
actual typealias MyThreadLocal = kotlin.native.concurrent.ThreadLocal
