// !LANGUAGE: +NewInference, +FunctionReferenceWithDefaultValueAsOtherType
import Host.importedObjectMemberWithVarargs

fun use(fn: (Int) -> String) = fn(1)

fun coerceToUnit(fn: (Int) -> Unit) {}

fun fnWithDefault(a: Int, b: Int = 42) = "abc"

fun fnWithVarargs(vararg xs: Int) = "abc"

object Host {
    fun importedObjectMemberWithVarargs(vararg xs: Int) = "abc"
}

fun testDefault() = use(::fnWithDefault)

fun testVararg() = use(::fnWithVarargs)

fun testCoercionToUnit() = coerceToUnit(::fnWithDefault)

fun testImportedObjectMember() = use(::importedObjectMemberWithVarargs)