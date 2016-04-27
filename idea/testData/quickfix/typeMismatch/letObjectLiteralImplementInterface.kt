// "Let the anonymous object implement interface 'Runnable'" "true"

fun foo(r: Runnable) {}

fun bar() {
    foo(<caret>object: {})
}

interface Runnable