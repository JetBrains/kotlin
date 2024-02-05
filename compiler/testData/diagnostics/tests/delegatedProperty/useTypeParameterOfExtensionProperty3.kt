// FIR_IDENTICAL
// ISSUE: KT-64102
// !DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// !LANGUAGE: +ForbidUsingExtensionPropertyTypeParameterInDelegate
// WITH_REFLECT

import kotlin.reflect.KProperty

class Inv<T>(var t: T)

open class Delegate<X> {
    var x: X? = null
    operator fun getValue(thisRef: Inv<X>, property: KProperty<*>) {
        val _x = x
        if (_x != null) {
            thisRef.t = _x
        } else {
            x = thisRef.t
        }
    }
}

val <T> Inv<T>.x: Unit <!DELEGATE_USES_EXTENSION_PROPERTY_TYPE_PARAMETER_ERROR!>by object : Delegate<T>() {}<!>