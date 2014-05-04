fun box(): String {
    val sum = {Int.(other: Int) -> this + other }
    return if (1 sum 2 == 3) "OK" else "Fail"
}
