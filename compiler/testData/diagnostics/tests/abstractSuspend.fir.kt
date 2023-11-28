// ISSUE: KT-63233

interface A {
    suspend fun foo()
}

class B: A {} //k1 - error, k2 - no error
