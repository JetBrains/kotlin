package test

data class Data(val first: Int = <expr>1</expr>, val second: String) {
    val property: Int = first

    fun member(): Int = property
}
