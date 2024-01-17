// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h

// FILE: objclib.h
#import <objc/NSObject.h>

static NSObject* __weak globalObject = nil;

void setObject(NSObject* obj) {
    globalObject = obj;
}

// Make sure this function persists, because the test expects to find this function in the stack trace.
__attribute__((noinline))
bool isObjectAliveShouldCrash() {
    return globalObject != nil;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(ObsoleteWorkersApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.native.concurrent.*
import kotlin.test.*
import kotlinx.cinterop.autoreleasepool
import objclib.*

val sb = StringBuilder()

fun box(): String {
    autoreleasepool {
        run()
    }
    // Experimental MM supports arbitrary object sharing.
    assertEquals("Before\nAfter true\n", sb.toString())
    return "OK"
}

private class NSObjectImpl : NSObject() {
    var x = 111
}

fun run() = withWorker {
    val obj = NSObjectImpl()
    setObject(obj)

    sb.appendLine("Before")
    val isAlive = try {
        execute(TransferMode.SAFE, {}) {
            isObjectAliveShouldCrash()
        }.result
    } catch (e: Throwable) {
        false
    }
    sb.appendLine("After $isAlive")
}
