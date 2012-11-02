package test

import org.jetbrains.annotations.NotNull

public open class AddNotNullJavaSubtype : java.lang.Object() {
    public open fun foo(): CharSequence? = throw UnsupportedOperationException()

    public open class Sub: AddNotNullJavaSubtype() {
        override fun foo(): String = throw UnsupportedOperationException()
    }
}