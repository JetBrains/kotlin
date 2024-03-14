// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val map: Map<Any, Any>)

expect value class <!NO_ACTUAL_FOR_EXPECT!>B<!>(val map: MutableMap<Any, Any>)

expect value class <!NO_ACTUAL_FOR_EXPECT!>C<!>(val f: () -> Int)

expect value class <!NO_ACTUAL_FOR_EXPECT!>D<!>(val i: Int)

// MODULE: jvm()()(common)
// FILE: jvm.kt
import kotlin.properties.Delegates

@JvmInline
actual value class A(val map: Map<Any, Any>) {
    val x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by map<!>
}

@JvmInline
actual value class B(val map: MutableMap<Any, Any>) {
    var x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by map<!>
}

@JvmInline
actual value class C(val f: () -> Int) {
    val x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by lazy { 0 }<!>
    val y <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by lazy(f)<!>
}

@JvmInline
actual value class D(val i: Int) {
    var x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by Delegates.observable(0) { _, _, _ -> i }<!>
}
