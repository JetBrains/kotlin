// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class A(val map: Map<Any, Any>)

expect value class B(val map: MutableMap<Any, Any>)

expect value class C(val f: () -> Int)

expect value class D(val i: Int)

// MODULE: jvm()()(common)
// FILE: jvm.kt
import kotlin.properties.Delegates

@JvmInline
actual value class A(val map: Map<Any, Any>) {
    val x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>map<!>
}

@JvmInline
actual value class B(val map: MutableMap<Any, Any>) {
    var x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>map<!>
}

@JvmInline
actual value class C(val f: () -> Int) {
    val x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy { 0 }<!>
    val y by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>lazy(f)<!>
}

@JvmInline
actual value class D(val i: Int) {
    var x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>Delegates.observable(0) { _, _, _ -> i }<!>
}
