package org.example

import kotlinx.serialization.Serializable

@Serializable
class SubClass : Base() {
    init {
        println(1)
        println(2)
        println(3)
    }
}
