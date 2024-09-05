// CODE_COMPILATION_EXCEPTION

// MODULE: ui
// MODULE_KIND: LibraryBinary
// FILE: com/example/ui/Text.kt
package com.example.ui

fun Text(text: String) {}

// MODULE: module2
// TARGET_PLATFORM: Common
// FILE: com/example/module2/A.kt
package com.example.module2

inline fun a(): String = "Hi" + b()
// FILE: com/example/module2/B.kt
package com.example.module2

inline fun b(): String = "Hi" + c()
// FILE: com/example/module2/C.kt
package com.example.module2

inline fun c(): String = "Hi" + a()

// MODULE: module1(module2)
// TARGET_PLATFORM: Common
// FILE: com/example/module1/moduleClass1.kt
@file:JvmName("SpecialName")
package com.example.module1

import com.example.module2.a

class moduleClass1 {
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

// MODULE: main(module1, ui)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.module1.moduleClass1
import com.example.ui.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + moduleClass1.giveMeString()
    )
}
