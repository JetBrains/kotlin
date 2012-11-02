package test

import org.jetbrains.annotations.NotNull

public open class InheritNullabilitySameJavaType : java.lang.Object() {
    public open fun foo(): CharSequence = throw UnsupportedOperationException()

    public open class Sub: InheritNullabilitySameJavaType() {
        override fun foo(): CharSequence = throw UnsupportedOperationException()
    }
}