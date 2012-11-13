package test

import org.jetbrains.annotations.NotNull

public open class InheritVariance : java.lang.Object() {
    public open fun foo(): MutableCollection<out Number> = throw UnsupportedOperationException()

    public open class Sub: InheritVariance() {
        override fun foo(): MutableList<out Number> = throw UnsupportedOperationException()
    }
}
