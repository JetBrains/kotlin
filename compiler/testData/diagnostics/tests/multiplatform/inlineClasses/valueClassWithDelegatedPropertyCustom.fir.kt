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

expect value class A(val i: Int)

expect value class B(val classWithDelegate: ClassWithDelegate)

expect value class C(val delegate: Delegate)

expect value class D(val property: ReadOnlyProperty<Any?, KProperty<*>>)

// MODULE: jvm()()(common)
// FILE: jvm.kt
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@JvmInline
actual value class A(val i: Int) {
    val x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>this::i<!>

    val j
        get() = 0
    val y by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>::j<!>

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = 0
    val z by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>this<!>
}

@JvmInline
actual value class B(val classWithDelegate: ClassWithDelegate) {
    val x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>NamedObject<!>
    val y by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>::topLevelProperty<!>
    val z by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>classWithDelegate::i<!>
}

@JvmInline
actual value class C(val delegate: Delegate) {
    val x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>delegate<!>
}

@JvmInline
actual value class D(val property: ReadOnlyProperty<Any?, KProperty<*>>) {
    val x by <!DELEGATED_PROPERTY_INSIDE_VALUE_CLASS!>property<!>
}
