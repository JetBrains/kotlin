// COMPILATION_ERRORS
companion {}

class C {
    private companion {}
    @Ann companion {}
    companion inline {}
}

fun foo() {
    companion {}
}

class C {
    companion {
        class Nested

        typealias TA
    }
}