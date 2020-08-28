// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun test(b: TestRepo) {
    coEvery1 { b.save(any()) }
}

fun <T> coEvery1(stubBlock: suspend MockKMatcherScope.() -> T) {}

class MockKMatcherScope {
    inline fun <reified T : Any> any(): T = TODO()
}

class TestRepo : CrudRepository<Int, String>

interface CrudRepository<T, K> {
    fun <S : T?> save(entity: S): S = TODO()
}