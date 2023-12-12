// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: leakMemoryWithRunningThreadUnchecked.def
---
void test_RunInNewThread(void (*)());

// FILE: leakMemoryWithRunningThreadUnchecked.h
#ifdef __cplusplus
extern "C" {
#endif

void test_RunInNewThread(void (*)());

#ifdef __cplusplus
}
#endif

// FILE: leakMemoryWithRunningThreadUnchecked.cpp
#include "leakMemoryWithRunningThreadUnchecked.h"

#include <atomic>
#include <thread>

extern "C" void test_RunInNewThread(void (*f)()) {
    std::atomic<bool> haveRun(false);
    std::thread t([f, &haveRun]() {
        f();
        haveRun = true;
        while (true) {}
    });
    t.detach();
    while (!haveRun) {}
}


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlin.native.runtime.NativeRuntimeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class
)

import leakMemoryWithRunningThreadUnchecked.*
import kotlin.concurrent.AtomicInt
import kotlin.native.concurrent.*
import kotlin.native.Platform
import kotlin.test.*
import kotlinx.cinterop.*

val global = AtomicInt(0)

fun ensureInititalized() {
    // Only needed with the legacy MM. TODO: Remove when legacy MM is gone.
    kotlin.native.initRuntimeIfNeeded()
    // Leak memory
    StableRef.create(Any())
    global.value = 1
}

fun box(): String {
    Platform.isMemoryLeakCheckerActive = true
    kotlin.native.runtime.Debugging.forceCheckedShutdown = false
    assertTrue(global.value == 0)
    // Created a thread, made sure Kotlin is initialized there.
    test_RunInNewThread(staticCFunction(::ensureInititalized))
    assertTrue(global.value == 1)
    // Now exiting. With unchecked shutdown, we exit quietly, even though there're
    // unfinished threads with runtimes.

    return "OK"
}
