fun test(): String {
    val b = A<String>().B<Int, Double>()
    val x: String? = b.getAE()
    val y: Int? = b.getBT()
    val z: Double? = b.getBE()

    // This line is needed to ensure that B.getAE's return type is not an error type; if it was, this line would compile with no errors
    b.getAE().unresolved()

    return "$x$y$z"
}
