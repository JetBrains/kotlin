// "Make 'foo' private" "false"
// ACTION: Convert parameter to receiver
// ACTION: Make 'Nested' internal
// ACTION: Make 'Nested' public
// ACTION: Remove parameter 'arg'
// ERROR: 'internal' function exposes its 'private' parameter type argument Nested
// ERROR: Cannot access 'Nested': it is private in 'Outer'

class Outer {
    private class Nested
}

class Generic<T>

internal fun foo(<caret>arg: Generic<Outer.Nested>) {}
