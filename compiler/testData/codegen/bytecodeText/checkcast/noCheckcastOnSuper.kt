interface Tr {
    fun extra(): String = "e"
}

class N : Tr {
    override fun extra(): String = super.extra()
}

// 0 CHECKCAST
