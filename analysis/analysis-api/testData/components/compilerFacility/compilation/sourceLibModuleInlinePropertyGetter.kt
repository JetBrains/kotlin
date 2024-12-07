// DUMP_IR

// MODULE: jvmLib
// MODULE_KIND: LibraryBinary
// FILE: jvmLib.kt
package com.example.jvmLib

fun Text(text: String) {}


// MODULE: common
// TARGET_PLATFORM: Common
// FILE: common.kt
package com.example.myModule

class OtherModule {
    val message: String
        inline get() {
            return secret()
        }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}


// MODULE: main(jvmLib)()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.myModule.OtherModule
import com.example.jvmLib.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + OtherModule().message
    )
}
