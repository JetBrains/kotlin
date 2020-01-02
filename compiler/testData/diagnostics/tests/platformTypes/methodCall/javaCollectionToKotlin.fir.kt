// !DIAGNOSTICS: -UNUSED_PARAMETER

import java.util.*

fun takeJ(map: Map<Any, Any>) {}

fun test() {
    takeJ(HashMap())
}