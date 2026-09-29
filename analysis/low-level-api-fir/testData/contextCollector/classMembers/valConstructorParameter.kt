package test

class Owner(val property: Int = <expr>42</expr>) {
    val other: Int = property

    fun member(): Int = other
}
