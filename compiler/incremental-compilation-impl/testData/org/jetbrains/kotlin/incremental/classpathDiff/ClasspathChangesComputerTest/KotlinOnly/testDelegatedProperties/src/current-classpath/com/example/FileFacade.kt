package com.example

import kotlin.reflect.KProperty

var delegatedProperty: String by Delegate() // Added

class Delegate {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "<Delegated>"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}