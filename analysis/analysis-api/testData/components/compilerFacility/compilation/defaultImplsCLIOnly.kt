// MODULE: lib
// MODULE_KIND: LibraryBinary
// COMPILER_ARGUMENTS: -jvm-default=disable
// FILE: p3/a.kt
package p3

interface KtInterfaceA {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()
}

// MODULE: lib2
// MODULE_KIND: LibraryBinary
// COMPILER_ARGUMENTS: -jvm-default=no-compatibility
// FILE: p2/b.kt
package p2

interface KtInterfaceB {
    fun defaultFun() {
        println("default")
    }

    fun withoutBody()
}

// MODULE: main(lib, lib2)
// MODULE_KIND: LibraryBinary
// COMPILER_ARGUMENTS: -jvm-default=disable
// FILE: main.kt
package home

import p3.KtInterfaceA
import p2.KtInterfaceB

fun test() {
    val a = object : KtInterfaceA {
        override fun withoutBody() {
            TODO("Not yet implemented")
        }
    }
    val b = object : KtInterfaceB {
        override fun withoutBody() {
            TODO("Not yet implemented")
        }
    }
}
