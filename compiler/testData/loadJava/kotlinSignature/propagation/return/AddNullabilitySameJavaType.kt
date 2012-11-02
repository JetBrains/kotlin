package test

import org.jetbrains.annotations.NotNull

public open class AddNullabilitySameJavaType : java.lang.Object() {
    public open fun foo(): CharSequence = throw UnsupportedOperationException()

    public open class Sub: AddNullabilitySameJavaType() {
        override fun foo(): CharSequence = throw UnsupportedOperationException()
    }
}