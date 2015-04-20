// !CHECK_TYPE

import kotlin.reflect.*

var x: Int = 42
val y: String get() = "y"

fun testX() {
    val xx = ::x
    checkSubtype<KMutableTopLevelProperty<Int>>(xx)
    checkSubtype<KMutableTopLevelVariable<Int>>(xx)
    checkSubtype<KTopLevelProperty<Int>>(xx)
    checkSubtype<KTopLevelVariable<Int>>(xx)
    checkSubtype<KMutableProperty<Int>>(xx)
    checkSubtype<KMutableVariable<Int>>(xx)
    checkSubtype<KProperty<Int>>(xx)
    checkSubtype<KCallable<Int>>(xx)

    checkSubtype<String>(xx.name)
    checkSubtype<Int>(xx.get())
    xx.set(239)
}

fun testY() {
    val yy = ::y
    checkSubtype<KMutableTopLevelProperty<String>>(<!TYPE_MISMATCH!>yy<!>)
    checkSubtype<KTopLevelVariable<String>>(yy)
    checkSubtype<KMutableProperty<String>>(<!TYPE_MISMATCH!>yy<!>)
    checkSubtype<KProperty<String>>(yy)
    checkSubtype<KCallable<String>>(yy)

    checkSubtype<String>(yy.name)
    checkSubtype<String>(yy.get())
    yy.<!UNRESOLVED_REFERENCE!>set<!>("yy")
}
