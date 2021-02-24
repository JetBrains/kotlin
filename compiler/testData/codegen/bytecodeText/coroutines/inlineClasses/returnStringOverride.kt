inline class IC(val s: String)

interface I {
    suspend fun returnAny(): Any
}

class C : I {
    override suspend fun returnAny(): IC = IC("OK")
}

// 1 INVOKESTATIC IC.box-impl
