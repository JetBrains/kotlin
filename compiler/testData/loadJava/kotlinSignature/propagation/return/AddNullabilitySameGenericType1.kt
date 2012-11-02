package test

import org.jetbrains.annotations.NotNull

public open class AddNullabilitySameGenericType1 : java.lang.Object() {
    public open fun foo(): MutableList<String> = throw UnsupportedOperationException()

    public open class Sub: AddNullabilitySameGenericType1() {
        override fun foo(): MutableList<String> = throw UnsupportedOperationException()
    }
}