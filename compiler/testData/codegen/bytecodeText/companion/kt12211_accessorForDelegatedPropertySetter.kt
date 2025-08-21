// WITH_STDLIB

import kotlin.properties.Delegates

class A {
    companion object {
        var x: String by Delegates.notNull<String>()
    }
}

// There should only be one putstatic in `A.<clinit>`
// 1 PUTSTATIC A.x\$delegate
