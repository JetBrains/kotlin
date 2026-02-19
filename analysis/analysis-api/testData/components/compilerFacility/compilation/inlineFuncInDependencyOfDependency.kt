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

class OtherModule {
    inline fun getInline() : String {
        return getPublished()
    }

    @PublishedApi
    internal fun getPublished() : String {
        return "foo"
    }
}


// MODULE: dep()()(commonDep)
// TARGET_PLATFORM: JVM


// MODULE: common(commonDep)
// FILE: common.kt
package com.example.common

import com.example.commonDep.OtherModule

fun foo(name: String) : String = "$name!" + OtherModule().getInline()


// MODULE: main(jvmLib, commonDep, dep)()(common)
// TARGET_PLATFORM: JVM
// FILE: main.kt
package home

import com.example.common.foo
import com.example.jvmLib.Text

fun Greeting(name: String) {
    Text(
        text = foo(name)
    )
}
