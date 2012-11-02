package test

import org.jetbrains.annotations.NotNull

public open class AddNotNullSameJavaType : java.lang.Object() {
    public open fun foo(): CharSequence? = throw UnsupportedOperationException()

    public open class Sub: AddNotNullSameJavaType() {
        override fun foo(): CharSequence = throw UnsupportedOperationException()
    }
}