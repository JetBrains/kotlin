// FILE: annotation.kt
package kotlin.native.concurrent

import kotlin.reflect.KProperty

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class SharedImmutable

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class ThreadLocal

// FILE: test.kt
import kotlin.native.concurrent.SharedImmutable
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KProperty

fun println(value: Int) {}
fun println(value: String) {}
fun println(value: Point) {}

data class Point(val x: Double, val y: Double)
@SharedImmutable
val point1 = Point(1.0, 1.0)

@SharedImmutable
var point2 = Point(2.0, 2.0)

class Date(@SharedImmutable val month: Int, @SharedImmutable var day:Int)
class Person(val name: String) {
    @SharedImmutable
    var surname: String? = null
}

class Figure {
    @SharedImmutable
    val cornerPoint: Point
        get() = point1
}

@SharedImmutable
var age = 20
    get() {
        println("Age is: $field")
        return field
    }
    set(value) {
        println(value)
    }

var globalAge = 30
@SharedImmutable
var age1 = 20
    get() {
        println("Age is: $field")
        return field
    }
    set(value) {
        globalAge = value
    }

@SharedImmutable
val age2 = 20
    get() {
        println("Age is: $field")
        return field
    }

@SharedImmutable
var point3: Point
    get() = point2
    set(value) {
        point2 = value
    }

@SharedImmutable
var point4: Point
    get() = point2
    set(value) {
        println(value)
    }

@ThreadLocal
var point0 = Point(2.0, 2.0)

@SharedImmutable
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

@SharedImmutable
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

@SharedImmutable
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

@SharedImmutable
var property2: Int by Delegate2()

@SharedImmutable
val someValue: Int
    get() = 20

@SharedImmutable
val someValueWithDelegate by Delegate()
