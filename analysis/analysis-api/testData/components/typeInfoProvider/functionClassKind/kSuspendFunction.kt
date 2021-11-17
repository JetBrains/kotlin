// WITH_REFLECT

import kotlin.reflect.KSuspendFunction0

fun foo(pause : suspend () -> Unit) {
    pause()
}

fun bar() {
    foo(x<caret>y as KSuspendFunction0<Unit>)
}
