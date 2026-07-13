// LANGUAGE: +FullValueClasses
// WITH_STDLIB

val list = mutableListOf<Any>()

interface Describable {
    fun describe(): String
}

abstract value class AbsDescribable(a: Int) {
    init {
        list.add("AbsDescribable($a)")
    }
}

// Value class that extends abstract value class and delegates interface via `by`
value class VCWithDelegation(val d: Describable) : AbsDescribable(42), Describable by d {
    init {
        list.add("VCWithDelegation(${d.describe()})")
    }
}

// Regular class that extends abstract value class and delegates interface via `by`
class RegularWithDelegation(val d: Describable) : AbsDescribable(99), Describable by d {
    init {
        list.add("RegularWithDelegation(${d.describe()})")
    }
}

// Sealed value class with interface, child delegates via `by`
sealed value class SealedDescribable(a: Int) : Describable {
    init {
        list.add("SealedDescribable($a)")
    }
}

value class SealedChildWithDelegation(val d: Describable) : SealedDescribable(7), Describable by d {
    init {
        list.add("SealedChildWithDelegation(${d.describe()})")
    }
}

class DescribableImpl(val tag: String) : Describable {
    override fun describe() = tag
}

fun box(): String {
    // --- Value class with `by` delegation ---
    list.clear()
    val descImpl = DescribableImpl("hello")
    val vcd = VCWithDelegation(descImpl)
    // AbsDescribable.init(42), VCWithDelegation(hello)
    require(list == listOf("AbsDescribable(42)", "VCWithDelegation(hello)")) { "VCWithDelegation: $list" }
    require(vcd.describe() == "hello") { "VCWithDelegation.describe() = ${vcd.describe()}" }

    // --- Regular class with `by` delegation and abstract value class parent ---
    list.clear()
    val rwd = RegularWithDelegation(DescribableImpl("world"))
    // AbsDescribable.init(99), RegularWithDelegation(world)
    require(list == listOf("AbsDescribable(99)", "RegularWithDelegation(world)")) { "RegularWithDelegation: $list" }
    require(rwd.describe() == "world") { "RegularWithDelegation.describe() = ${rwd.describe()}" }

    // --- Sealed value class with `by` delegation ---
    list.clear()
    val scwd = SealedChildWithDelegation(DescribableImpl("test"))
    // SealedDescribable.init(7), SealedChildWithDelegation(test)
    require(list == listOf("SealedDescribable(7)", "SealedChildWithDelegation(test)")) { "SealedChildWithDelegation: $list" }
    require(scwd.describe() == "test") { "SealedChildWithDelegation.describe() = ${scwd.describe()}" }

    return "OK"
}
