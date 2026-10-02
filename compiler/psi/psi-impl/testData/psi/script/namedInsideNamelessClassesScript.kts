// COMPILATION_ERRORS
package one.two

object {
    class A
    object B
    typealias C = Int
}

class Outer {
    object {
        class A
        object B
        typealias C = Int
    }
}
