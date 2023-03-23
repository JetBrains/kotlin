// SKIP_TXT
// FIR_IDENTICAL
// ISSUE: KT-57092, KT-57202

// FILE: A.kt

interface InterfaceWithDefault {
    val hostKind: Int get() = 24
}

interface SubInterfaceWithoutDefault<T> : InterfaceWithDefault {
    // SubInterfaceWithoutDefault.hostKind subsumes InterfaceWithDefault.hostKind, therefore no error.
    override val hostKind: Int
}

open class ClassWithDefault : InterfaceWithDefault {
    override val hostKind: Int get() = 42
}

// FILE: B.java

public class InheritsAll extends ClassWithDefault() implements SubInterfaceWithoutDefault, InterfaceWithDefault {}

public interface SubInterfaceWithoutDefaultRawSubtype implements SubInterfaceWithoutDefault {}

// FILE: C.kt

fun test(it: InheritsAll) {
    val kind = it.hostKind
}

class InheritsAllKotlin1 : InheritsAll()

class InheritsAllKotlin2 : ClassWithDefault(), SubInterfaceWithoutDefaultRawSubtype, InterfaceWithDefault
