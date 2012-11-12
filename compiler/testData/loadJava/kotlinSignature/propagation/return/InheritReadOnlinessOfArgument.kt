package test

import org.jetbrains.annotations.NotNull

public open class InheritReadOnlinessOfArgument : java.lang.Object() {
    public open fun foo(): List<List<String>> = throw UnsupportedOperationException()

    public open class Sub: InheritReadOnlinessOfArgument() {
        override fun foo(): List<List<String>> = throw UnsupportedOperationException()
    }
}
