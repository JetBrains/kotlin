// COMPILATION_ERRORS
package one.two

class {
    companion object {
        class A
    }
}

object {
    class Named {
        companion object NamedCompanion {
            class B
            object {
                class C
            }
        }
    }
}

class Outer {
    object {
        class Inner {
            companion {
                class D
                typealias E = Int
            }
        }
    }

    companion {
        object {
            class F
        }
    }
}
