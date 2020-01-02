interface IA

interface IB : IA {
    override fun toString(): String = "IB"
}

interface IC : IB {
    override fun toString(): String = "IC"
}

interface ID : IC {
    override fun toString(): String = "ID"
}