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
package com.example.myModule

class OtherModule {
    companion object {
        inline fun giveMeString(): String {
            return secret()
        }

        @PublishedApi
        internal fun secret(): String {
            return "what is up!!!!!!!"
        }
    }

    companion object Named {
        inline fun giveMeString(): String {
            return secret()
        }

        @PublishedApi
        internal fun secret(): String {
            return "what is up!!!!!!!"
        }
    }
}

object Another {
    inline fun giveMeString(): String {
        return secret()
    }

    @PublishedApi
    internal fun secret(): String {
        return "what is up!!!!!!!"
    }
}

// MODULE: main(jvmLib)()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.myModule.Another
import com.example.myModule.OtherModule
import com.example.jvmLib.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + OtherModule.giveMeString() + OtherModule.Named.giveMeString() + Another.giveMeString()
    )
}
