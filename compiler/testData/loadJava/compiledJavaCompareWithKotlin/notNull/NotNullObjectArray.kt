package test

public open class NotNullObjectArray() {
    public open fun hi(): Array<Any> = throw Exception()
}
