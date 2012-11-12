package test

import org.jetbrains.annotations.NotNull

public open class InheritReadOnlinessSubclass : java.lang.Object() {
    public open fun foo(): Collection<String> = throw UnsupportedOperationException()

    public open class Sub: InheritReadOnlinessSubclass() {
        override fun foo(): List<String> = throw UnsupportedOperationException()
    }
}
