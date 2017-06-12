package test

typealias Str = String
typealias L<T> = List<T>

class Klass {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias Nested = Z

    class Z
}
