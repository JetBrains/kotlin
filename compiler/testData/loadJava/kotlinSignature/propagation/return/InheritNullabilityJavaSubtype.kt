package test

import org.jetbrains.annotations.NotNull

public open class InheritNullabilityJavaSubtype : java.lang.Object() {
    public open fun foo(): CharSequence = throw UnsupportedOperationException()

    public open class Sub: InheritNullabilityJavaSubtype() {
        override fun foo(): String = throw UnsupportedOperationException()
    }
}