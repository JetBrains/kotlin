package org.jetbrains.annotations

import org.jetbrains.annotations.PropertyKey

fun message(@PropertyKey(resourceBundle = "MessageBundle1.dependency") key: String) = key

fun test() {
    message("<caret>")
}

// ELEMENT: foo.bar
// CHARS: 'foo\n'
