// VALHALLA_SUPPORT: ALL_VALUES
// LANGUAGE: +FullValueClasses
// CHECK_BYTECODE_TEXT

@WillBecomeValue
class WillBecomeVal(val a: Int, val b: Int) {
    override fun equals(other: Any?): Boolean = other is WillBecomeVal && other.a == a && other.b == b
    override fun hashCode(): Int = 31 * a + b
    override fun toString(): String = "WillBecomeVal($a, $b)"
}

class Identity(val a: Int)

class Holder(val willBecomeVal: WillBecomeVal, val identity: Identity, val prim: Int)

fun box(): String {
    val holder = Holder(WillBecomeVal(1, 2), Identity(3), 4)
    if (holder.willBecomeVal != WillBecomeVal(1, 2)) return "Holder.willBecomeVal: ${holder.willBecomeVal}"
    if (holder.identity.a != 3) return "Holder.identity: ${holder.identity.a}"
    if (holder.prim != 4) return "Holder.prim: ${holder.prim}"
    return "OK"
}

// 1 ATTRIBUTE LoadableDescriptors
// 1 ATTRIBUTE LoadableDescriptors : LWillBecomeVal;\n
