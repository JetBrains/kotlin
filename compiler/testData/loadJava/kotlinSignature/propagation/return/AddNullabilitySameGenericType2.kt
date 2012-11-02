package test

import org.jetbrains.annotations.NotNull

public open class AddNullabilitySameGenericType2 : java.lang.Object() {
    public open fun foo(): MutableList<String> = throw UnsupportedOperationException()

    public open class Sub: AddNullabilitySameGenericType2() {
        override fun foo(): MutableList<String> = throw UnsupportedOperationException()
    }
}