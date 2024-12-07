// DUMP_IR

// MODULE: jvmLib
// MODULE_KIND: LibraryBinary
// FILE: jvmLib.kt
package com.example.jvmLib

fun Text(text: String) {}

//       _main_
//      /      \
//     /        \
// module1   module2
//     \        /
//      \      /
//       module3_________
//      /      \         \
// module4   module5     module6
//      \      /         /  /
//       module7--------/  /
//         |              /
//       module8---------/
//
// post-order = {8, 7, 4, 5, 6, 3, 1, 2, main}

// MODULE: module8
// TARGET_PLATFORM: Common
// FILE: com/example/module8/moduleClass8.kt
package com.example.module8

class moduleClass8 {
    companion object {
        inline fun giveMeString(): String {
            return secret()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo"
        }
    }
}


// MODULE: jvm8()()(module8)
// TARGET_PLATFORM: JVM


// MODULE: module7(module8)
// TARGET_PLATFORM: Common
// FILE: com/example/module7/moduleClass7.kt
package com.example.module7

import com.example.module8.moduleClass8

class moduleClass7 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass8.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo"
        }
    }
}


// MODULE: jvm7(module8, jvm8)()(module7)
// TARGET_PLATFORM: JVM


// MODULE: module6(module7, module8)
// TARGET_PLATFORM: Common
// FILE: com/example/module6/moduleClass6.kt
package com.example.module6

import com.example.module7.moduleClass7
import com.example.module8.moduleClass8

class moduleClass6 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass7.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo" + moduleClass8.giveMeString()
        }
    }
}


// MODULE: jvm6(module7, jvm7, module8, jvm8)()(module6)
// TARGET_PLATFORM: JVM


// MODULE: module5(module7)
// TARGET_PLATFORM: Common
// FILE: com/example/module5/moduleClass5.kt
package com.example.module5

import com.example.module7.moduleClass7

class moduleClass5 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass7.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo"
        }
    }
}


// MODULE: jvm5(module7, jvm7)()(module5)
// TARGET_PLATFORM: JVM


// MODULE: module4(module7)
// TARGET_PLATFORM: Common
// FILE: com/example/module4/moduleClass4.kt
package com.example.module4

import com.example.module7.moduleClass7

class moduleClass4 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass7.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo"
        }
    }
}


// MODULE: jvm4(module7, jvm7)()(module4)
// TARGET_PLATFORM: JVM


// MODULE: module3(module4, module5, module6)
// TARGET_PLATFORM: Common
// FILE: com/example/module3/moduleClass3.kt
package com.example.module3

import com.example.module4.moduleClass4
import com.example.module5.moduleClass5
import com.example.module6.moduleClass6

class moduleClass3 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass4.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo" + moduleClass5.giveMeString() + moduleClass6.giveMeString()
        }
    }
}


// MODULE: jvm3(module4, jvm4, module5, jvm5, module6, jvm6)()(module3)
// TARGET_PLATFORM: JVM


// MODULE: module2(module3)
// TARGET_PLATFORM: Common
// FILE: com/example/module2/moduleClass2.kt
package com.example.module2

import com.example.module3.moduleClass3

class moduleClass2 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass3.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo"
        }
    }
}


// MODULE: jvm2(module3, jvm3)()(module2)
// TARGET_PLATFORM: JVM


// MODULE: module1(module3)
// TARGET_PLATFORM: Common
// FILE: com/example/module1/moduleClass1.kt
@file:JvmName("SpecialName")
package com.example.module1

import com.example.module3.moduleClass3

class moduleClass1 {
    companion object {
        inline fun giveMeString(): String {
            return secret() + moduleClass3.giveMeString()
        }

        @PublishedApi
        internal fun secret(): String {
            return "foo"
        }
    }
}


// MODULE: jvm1(module3, jvm3)()(module1)
// TARGET_PLATFORM: JVM
// FILE: com/example/module1/jvm1.kt
package com.example.module1
class Boo


// MODULE: main(module1, jvm1, module2, jvm2, jvmLib)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.module1.moduleClass1
import com.example.module2.moduleClass2
import com.example.jvmLib.Text

fun Greeting(name: String) {
    Text(
        text = "$name!" + moduleClass1.giveMeString() + moduleClass2.giveMeString()
    )
}
