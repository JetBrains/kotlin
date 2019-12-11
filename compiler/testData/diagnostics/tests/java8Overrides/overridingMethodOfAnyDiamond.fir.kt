interface ILeft {
    override fun toString(): String
}

interface IRight {
    override fun toString(): String
}

interface IDiamond : ILeft, IRight {
    override fun toString(): String = "IDiamond"
}