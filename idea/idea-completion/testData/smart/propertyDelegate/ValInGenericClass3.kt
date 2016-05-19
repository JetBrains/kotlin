import kotlin.reflect.KProperty

class X<T>(t: T) {
    operator fun getValue(thisRef: C<String, T>, property: KProperty<*>): T = throw Exception()
}

class C<T1, T2> {
    val property: T2 by <caret>
}

// EXIST: lazy
// ABSENT: X
