// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt63049.def
depends = Foundation
language = Objective-C
---
@interface WithClassProperty
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import kt63049.*
import kotlin.test.assertEquals

class Impl : WithClassProperty() {
    companion object : WithClassPropertyMeta() {
        fun stringProperty(): String? = "OK"
    }
}

fun box() = Impl.stringProperty()
