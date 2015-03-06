package dependency

class D {
    inner class Inner {
        inner class Inner
    }
    open class Nested {
        class Nested
    }

    default object {
        class NestedInClassObject
    }
}
