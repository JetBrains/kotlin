// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

package test.regressions.kt1172

import kotlin.concurrent.*
import java.util.*

public fun scheduleRefresh(vararg files : Object) {
    java.util.ArrayList<Object>(files.map{ it })
}

fun box(): String {
    scheduleRefresh()
    return "OK"
}
