package test

import dependency.*

public enum class Enum : Tr {
    ONE
    TWO
    THREE {
        fun g() {
        }
    }
}
