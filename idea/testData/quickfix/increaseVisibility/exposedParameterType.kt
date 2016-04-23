// "Make 'Nested' internal" "true"
// ACTION: Make 'Nested' public
// ACTION: Make 'foo' private

class Outer {
    private class Nested
}

class Generic<T>

internal fun foo(<caret>arg: Generic<Outer.Nested>) {}