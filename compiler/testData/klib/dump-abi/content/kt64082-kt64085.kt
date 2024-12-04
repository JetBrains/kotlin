// MODULE: kt64082_kt64085

class ConstHolder1 {
    private fun local() {
        println(CONST_VAL)
    }

    companion object {
        const val CONST_VAL: String = ""
    }
}

class ConstHolder2 {
    companion object {
        const val CONST_VAL: String = ""
    }
}

class Reader {
    private val properties: Named

    init {
        properties = NamedImpl()
        properties.name
    }
}

interface Named {
    val name: String
}

class NamedImpl : Named {
    override val name: String = ""
}
