// !DIAGNOSTICS: -UNUSED_PARAMETER
// SKIP_TXT

import kotlin.reflect.KProperty

class A {
    suspend operator fun get(x: Int) = 1
    suspend operator fun set(x: Int, v: String) {}

    operator suspend fun contains(y: String): Boolean = true

    suspend operator fun unaryPlus() = this
    suspend operator fun unaryMinus() = this
    suspend operator fun not() = this
    suspend operator fun inc() = this
    suspend operator fun dec() = this

    suspend operator fun plus(a: A) = a
    suspend operator fun minus(a: A) = a
    suspend operator fun times(a: A) = a
    suspend operator fun div(a: A) = a
    suspend operator fun rem(a: A) = a
    suspend operator fun rangeTo(a: A) = a

    suspend operator fun invoke(a: A) = a

    suspend operator fun compareTo(a: A) = hashCode().compareTo(a.hashCode())

    suspend operator fun iterator() = this
    suspend operator fun hasNext() = false
    suspend operator fun next() = this

    suspend operator fun contains(b: A) = this == b
    suspend operator fun get(a: A) = a
    suspend <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(a: A) = a === this
    suspend operator fun set(a: A, b: A) {}

    suspend <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun provideDelegate(a: A, p: KProperty<*>) = a
    suspend <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun getValue(a: A, p: KProperty<*>) = a
    suspend <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun setValue(a: A, p: KProperty<*>, b: A) {}
}

class B
suspend operator fun B.get(x: Int) =1
suspend operator fun B.set(x: Int, v: String) {}

operator suspend fun B.contains(y: String): Boolean = true

class C {
    suspend fun get(x: Int) = 1
    suspend fun set(x: Int, v: String) {}

    suspend fun contains(y: String): Boolean = true
}

class D
suspend fun D.get(x: Int) =1
suspend fun D.set(x: Int, v: String) {}

suspend fun D.contains(y: String): Boolean = true
