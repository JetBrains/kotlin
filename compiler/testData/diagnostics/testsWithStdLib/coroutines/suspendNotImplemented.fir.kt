// ISSUE: KT-63233, KT-59818

interface A {
    suspend fun foo()
}

class B : A
