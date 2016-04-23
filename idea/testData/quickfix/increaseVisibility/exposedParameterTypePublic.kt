// "Make 'Nested' public" "true"
// ACTION: Make 'Nested' internal

class Outer {
    private class Nested
}

class Generic<T>

internal fun foo(<caret>arg: Generic<Outer.Nested>) {}