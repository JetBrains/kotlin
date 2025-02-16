typealias Test1 = String

fun foo() {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "UNSUPPORTED")
    typealias TestLocal = String
}

class C {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY", "UNSUPPORTED_FEATURE")
    typealias TestNested = String
}

