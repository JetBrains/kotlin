class NonUsed(val x: Int) // NO
// NO
data class UsedInData(val x: Int)
// YES
class UsedInProperty(val x: Int) {
    val y = x
}
// YES
class UsedInInitializer(val x: Int) {
    val y: Int
    init {
        y = x
    }
}
// YES
class UsedInConstructor(val x: Int) {
    fun foo(arg: Int) = arg

    constructor(): this(42) {
        foo(x)
    }
}
// NO
class UsedInFunction(val x: Int) {
    fun get() = x
}
// NO
class UsedInGetter(val x: Int) {
    val y: Int
        get() = x
}
// NO
class UsedInSetter(val x: Int) {
    var y: Int
        get() = field
        set(arg) { field = x + arg }
}
// NO
class UsedInInnerClass(val x: Int) {
    inner class Inner {
        fun foo() = x
    }
}
// NO
class UsedOutside(val x: Int)

fun use(): Int {
    val used = UsedOutside(30)
    return used.x
}
// YES
class PrivateUsedInProperty(private val x: Int) {
    val y = x
}
// NO
open class Base(protected open val x: Int)
// NO
override class UsedOverridden(override val x: Int) {
    val y = x
}
// YES
class UsedInPropertyVar(var x: Int) {
    var y = x
}
// NO
class UsedInPropertyAnnotated(@JvmField val x: Int) {
    val y = x
}
// YES
class UsedWithoutThisInInitProperty(val x: Int) {
    init {
        val y = x
    }
}
// NO
class UsedWithThisInInitProperty(val x: Int) {
    init {
        val y = this.x
    }
}
// NO
class UsedWithLabeledThisInInitProperty(val x: Int) {
    init {
        run {
            val y = this@UsedWithLabeledThisInInitProperty.x
        }
    }
}
// NO
class UsedInFunctionProperty(val x: Int) {
    fun get() {
        val y = x
        return y
    }
}
// NO
class ModifiedInInit(var x: Int) {
    init {
        x += 2
    }
}
// NO
open class UsedInOverride(val x: Int)

class UserInOverride(override val x: Int) : UsedInOverride(x)
