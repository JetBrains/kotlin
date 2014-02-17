package test

public open class NotNullIntArray() : java.lang.Object() {
    public open fun hi(): IntArray = throw Exception()
}
