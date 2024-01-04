/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: cstdlib_setenv.def
headers = stdlib.h
compilerOpts.mingw = -D ADD_WINDOWS_ENV_FUNCTIONS

---

#ifdef ADD_WINDOWS_ENV_FUNCTIONS
static inline int setenv(const char *name, const char *value, int overwrite)
{
    int errcode = 0;
    if (!overwrite) {
        size_t envsize = 0;
        errcode = getenv_s(&envsize, NULL, 0, name);
        if(errcode || envsize) return errcode;
    }
    return _putenv_s(name, value);
}
static inline int unsetenv(const char *name)
{
    return _putenv_s(name, "");
}
#endif


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

import kotlin.native.*
import kotlin.test.*
import cstdlib_setenv.*
import kotlinx.cinterop.*


@OptIn(kotlin.ExperimentalStdlibApi::class)
fun box(): String {
    val platformProcessors: Int = Platform.getAvailableProcessors()
    val jvmProcessors: Int = platformProcessors // Ideally, must be value of Runtime.getRuntime().availableProcessors() from testsystem
    assertTrue(jvmProcessors > 0)
    assertTrue(platformProcessors > 0)
    assertEquals(jvmProcessors, platformProcessors)

    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "12345", 1)
    assertEquals(Platform.getAvailableProcessors(), 12345)
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", Long.MAX_VALUE.toString(), 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "-1", 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "0", 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    // windows doesn't support empty env variables
    if (Platform.osFamily != OsFamily.WINDOWS) {
        setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "", 1)
        assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    }
    setenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS", "123aaaa", 1)
    assertFailsWith<IllegalStateException> { Platform.getAvailableProcessors() }
    unsetenv("KOTLIN_NATIVE_AVAILABLE_PROCESSORS")
    assertEquals(Platform.getAvailableProcessors(), platformProcessors)

    return "OK"
}
