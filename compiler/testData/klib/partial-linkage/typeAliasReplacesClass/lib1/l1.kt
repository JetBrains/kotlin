open class Foo

interface Bar

interface ParentWithTypeParameter<T>
open class WithTypeParameter<T>(val x: T) : ParentWithTypeParameter<T>

class Outer {
    open class Nested
}
