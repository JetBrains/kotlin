// FIR_IDENTICAL
import Host.importedObjectMemberWithVarargs

fun use(fn: (Int) -> String) = fn(1)

fun use0(fn: () -> String) = fn()

fun coerceToUnit(fn: (Int) -> Unit) {}

fun fnWithDefault(a: Int, b: Int = 42) = "abc"

fun fnWithDefaults(a: Int = 1, b: Int = 2) = ""

fun fnWithVarargs(vararg xs: Int) = "abc"

object Host {
    fun importedObjectMemberWithVarargs(vararg xs: Int) = "abc"
}

fun testDefault() = use(::fnWithDefault)

fun testVararg() = use(::fnWithVarargs)

fun testCoercionToUnit() = coerceToUnit(::fnWithDefault)

fun testImportedObjectMember() = use(::importedObjectMemberWithVarargs)

fun testDefault0() = use0(::fnWithDefaults)

fun testVararg0() = use0(::fnWithVarargs)
