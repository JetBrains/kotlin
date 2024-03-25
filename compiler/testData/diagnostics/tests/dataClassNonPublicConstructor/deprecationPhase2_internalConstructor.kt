// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility

// MODULE: a
// FILE: a.kt
package a

data class Data internal constructor(val x: Int) {
    fun member() {
        copy()
        this.copy()
    }

    companion object {
        fun of(): Data {
            return Data(1).copy()
        }
    }
}

fun topLevel(data: Data) {
    data.copy()
}

fun Data.topLevelExtension() {
    copy()
}

fun local() {
    data class Local internal constructor(val x: Int)

    fun Local.foo() {
        copy()
    }
}

// MODULE: b(a)
// FILE: b.kt
package b

import a.Data

fun topLevel(data: Data) {
    data.copy()
}

fun Data.topLevelExtension() {
    copy()
}
