// IGNORE_BACKEND: JS

// SKIP_SOURCEMAP_REMAPPING

// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import COROUTINES_PACKAGE.*

fun box(): String {
    val name = ::coroutineContext.name
    if (name != "coroutineContext") return "Fail 1: $name"

    return "OK"
}
