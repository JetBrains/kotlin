package com.example

import kotlin.reflect.KProperty

class Delegate {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return "<Delegated>"
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
    }
}