package test

public open class NotNullObjectArray() : java.lang.Object() {
    public open fun hi(): Array<Any> = throw Exception()
}
