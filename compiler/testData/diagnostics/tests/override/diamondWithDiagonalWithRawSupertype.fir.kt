// SKIP_TXT
// ISSUE: KT-57092, KT-57202

// FILE: A.kt

class Holder<T>(var value: T)

interface InterfaceWithDefault {
    val hostKind: Int get() = 24
    val holder: Holder<Number> get() = Holder(24)
}

interface SubInterfaceWithoutDefault<T> : InterfaceWithDefault {
    // SubInterfaceWithoutDefault.hostKind subsumes InterfaceWithDefault.hostKind, therefore no error.
    override val hostKind: Int
    override val holder: Holder<Number>
}

open class ClassWithDefault : InterfaceWithDefault {
    override val hostKind: Int get() = 42
    override val holder: Holder<Number> get() = Holder(42)
}

// FILE: B.java

public class InheritsAll extends ClassWithDefault() implements SubInterfaceWithoutDefault, InterfaceWithDefault {}

public interface SubInterfaceWithoutDefaultRawSubtype implements SubInterfaceWithoutDefault {}

// FILE: C.kt

fun test(it: InheritsAll) {
    val kind = it.hostKind

    it.holder.value = 10
    it.holder.value = <!ASSIGNMENT_TYPE_MISMATCH!>"10"<!>

    val h1: Number = it.holder.value
    val h2: String = <!INITIALIZER_TYPE_MISMATCH!>it.holder.value<!>
}

class InheritsAllKotlin1 : InheritsAll()

class InheritsAllKotlin2 : ClassWithDefault(), SubInterfaceWithoutDefaultRawSubtype, InterfaceWithDefault
