// "Make Nested public" "true"

class Outer {
    private class Nested
}

class Generic<T>

internal fun foo(<caret>arg: Generic<Outer.Nested>) {}