interface I {
    suspend fun foo()
}

class C : I {
    <caret>
}
