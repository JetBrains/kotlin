// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt63049.def
depends = Foundation
language = Objective-C
headers = kt63049.h

// FILE: kt63049.h
#import "Foundation/NSObject.h"
@interface KT63049 : NSObject
@end

// FILE: kt63049.m
#import "kt63049.h"
@implementation KT63049 : NSObject
@end

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalObjCName::class)

import kt63049.*
import kotlin.test.assertEquals

class Impl : KT63049() {
    companion object : KT63049Meta() {
        fun stringProperty(): String? = "OK"
    }
}

fun box() = Impl.stringProperty()
