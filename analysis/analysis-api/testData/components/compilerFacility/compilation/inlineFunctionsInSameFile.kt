// DUMP_IR

// MODULE: jvmLib
// MODULE_KIND: LibraryBinary
// FILE: jvmLib.kt
package com.example.jvmLib

fun Text(text: String) {}


// MODULE: commonDep
// TARGET_PLATFORM: Common
// FILE: commonDep.kt
package com.example.commonDep

inline fun a(): String = "Hi" + b()
inline fun b(): String = "Hi" + c()
inline fun c(): String = "Hi"
inline fun d(): String = "Hi" + a()


// MODULE: dep()()(commonDep)
// TARGET_PLATFORM: JVM

// MODULE: common(commonDep)
// TARGET_PLATFORM: Common
// FILE: common.kt
@file:JvmName("SpecialName")
package com.example.common

import com.example.commonDep.a

class Foo {
    companion object {
        inline fun giveMeString(): String {
            return secret() + a()
        }

        @PublishedApi
        internal fun secret(): String {
            return "what is up!!!!!!!"
        }
    }
}


// MODULE: main(jvmLib, commonDep, dep)()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.common.Foo
import com.example.jvmLib.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + Foo.giveMeString()
    )
}
