package test

public open class NotNullIntArray() {
    public open fun hi(): IntArray = throw Exception()
}
