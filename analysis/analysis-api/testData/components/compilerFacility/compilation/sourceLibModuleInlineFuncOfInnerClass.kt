// DUMP_IR

// MODULE: jvmLib
// MODULE_KIND: LibraryBinary
// FILE: jvmLib.kt
package com.example.jvmLib

fun Text(text: String) {}


// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
@file:JvmName("SpecialName")
package com.example.common

class OtherModule {
    class Inner {
        inline fun getInline(): String {
            return getPublished()
        }

        @PublishedApi
        internal fun getPublished(): String {
            return "foo"
        }
    }
}


// MODULE: main(jvmLib)()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.common.OtherModule
import com.example.jvmLib.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + OtherModule.Inner().getInline()
    )
}
