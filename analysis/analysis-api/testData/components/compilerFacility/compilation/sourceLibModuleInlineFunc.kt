// DUMP_IR

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: com/example/ui/Text.kt
package com.example.ui

fun Text(text: String) {}

// MODULE: myModule
// TARGET_PLATFORM: Common
// FILE: com/example/myModule/OtherModule.kt
package com.example.myModule

class OtherModule {
    inline fun giveMeString() : String {
        return secret()
    }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}

// MODULE: main(myModule, ui)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.myModule.OtherModule
import com.example.ui.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + OtherModule().giveMeString()
    )
}
