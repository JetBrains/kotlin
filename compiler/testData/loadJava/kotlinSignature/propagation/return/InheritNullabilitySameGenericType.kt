package test

import org.jetbrains.annotations.NotNull

public open class InheritNullabilitySameGenericType : java.lang.Object() {
    public open fun foo(): MutableList<String> = throw UnsupportedOperationException()

    public open class Sub: InheritNullabilitySameGenericType() {
        override fun foo(): MutableList<String> = throw UnsupportedOperationException()
    }
}