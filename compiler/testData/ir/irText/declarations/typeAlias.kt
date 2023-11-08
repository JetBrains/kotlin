// SEPARATE_SIGNATURE_DUMP_FOR_K2
// ^ KT-45285 (Support nested and local type aliases) -- partly implemented in K1 and not implemented in K2
// SKIP_SIGNATURE_VERIFICATION

typealias Test1 = String

fun foo() {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias TestLocal = String
}

class C {
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias TestNested = String
}
