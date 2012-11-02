package test

import org.jetbrains.annotations.NotNull

public open class AddNullabilityJavaSubtype : java.lang.Object() {
    public open fun foo(): CharSequence = throw UnsupportedOperationException()

    public open class Sub: AddNullabilityJavaSubtype() {
        override fun foo(): String = throw UnsupportedOperationException()
    }
}