package test

public open class MethodTypePTwoUpperBounds() : java.lang.Object() {
    public open fun <T> foo(): Unit where T : Cloneable?, T : Runnable? {
    }
}
