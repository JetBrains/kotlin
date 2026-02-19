// LANGUAGE: +MultiPlatformProjects

// MODULE: lib-common
// FILE: common.kt

package test

expect enum class E

// MODULE: lib()()(lib-common)
// FILE: jvm.kt

package test

actual typealias E = F

enum class F {
    OK;
}

// MODULE: main(lib)
// FILE: jvm2.kt

import test.E.*

fun box(): String {
    return OK.name
}
