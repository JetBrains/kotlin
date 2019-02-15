
expect interface Base

class Derived : Base

fun consumeCommonBase(cb: Base) {}
fun consumeCommonDerived(cd: Derived) {}

fun getCommonBase(): Base = null!!
fun getCommonDerived(): Derived = null!!
