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

// MODULE: moduleWithoutInline(myModule)
// FILE: com/example/moduleWithoutInline/Foo.kt
package com.example.moduleWithoutInline

import com.example.myModule.OtherModule

fun foo(name: String) : String = "$name!" + OtherModule().giveMeString()

// MODULE: main(moduleWithoutInline, ui)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.moduleWithoutInline.foo
import com.example.ui.Text

fun Greeting(name: String) {
    Text(
        text = foo(name)
    )
}
