package test

import org.jetbrains.annotations.NotNull

public open class InheritReadOnlinessSameClass : java.lang.Object() {
    public open fun foo(): List<String> = throw UnsupportedOperationException()

    public open class Sub: InheritReadOnlinessSameClass() {
        override fun foo(): List<String> = throw UnsupportedOperationException()
    }
}
