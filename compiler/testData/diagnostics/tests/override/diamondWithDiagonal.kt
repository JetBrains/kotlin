// SKIP_TXT
// FIR_IDENTICAL
// ISSUE: KT-57092

interface InterfaceWithDefault {
    val hostKind: Int get() = 24
}

interface SubInterfaceWithoutDefault : InterfaceWithDefault {
    // SubInterfaceWithoutDefault.hostKind subsumes InterfaceWithDefault.hostKind, therefore no error.
    override val hostKind: Int
}

open class ClassWithDefault : InterfaceWithDefault {
    override val hostKind: Int get() = 42
}

class InheritsAll :
    ClassWithDefault(),
    SubInterfaceWithoutDefault,
    InterfaceWithDefault
