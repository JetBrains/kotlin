// TARGET_BACKEND: JVM
package test

public open class MethodTypePTwoUpperBounds() {
    public open fun <T> foo(): Unit where T : Cloneable?, T : Runnable? {
    }
}
