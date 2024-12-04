data class Abc(val firstProperty: Int, val secondProperty: Double, val thirdProperty: String) {
    fun check() {}
    val Int.bodyProperty get() = 1L
}


// class: Abc