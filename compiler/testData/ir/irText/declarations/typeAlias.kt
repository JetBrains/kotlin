// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ KT-45285

typealias Test1 = String

fun foo() {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias TestLocal = String
}

class C {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias TestNested = String
}
