// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

object NamedObject {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 0
}

val topLevelProperty = 0

class ClassWithDelegate(val i: Int)

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 0
}

expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val i: Int)

expect value class <!NO_ACTUAL_FOR_EXPECT!>B<!>(val classWithDelegate: ClassWithDelegate)

expect value class <!NO_ACTUAL_FOR_EXPECT!>C<!>(val delegate: Delegate)

expect value class <!NO_ACTUAL_FOR_EXPECT!>D<!>(val property: ReadOnlyProperty<Any?, KProperty<*>>)

// MODULE: jvm()()(common)
// FILE: jvm.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@JvmInline
actual value class A(val i: Int) {
    val x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by this::i<!>

    val j
        get() = 0
    val y <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by ::j<!>

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 0
    val z <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by this<!>
}

@JvmInline
actual value class B(val classWithDelegate: ClassWithDelegate) {
    val x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by NamedObject<!>
    val y <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by ::topLevelProperty<!>
    val z <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by classWithDelegate::i<!>
}

@JvmInline
actual value class C(val delegate: Delegate) {
    val x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by delegate<!>
}

@JvmInline
actual value class D(val property: ReadOnlyProperty<Any?, KProperty<*>>) {
    val x <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>by property<!>
}
