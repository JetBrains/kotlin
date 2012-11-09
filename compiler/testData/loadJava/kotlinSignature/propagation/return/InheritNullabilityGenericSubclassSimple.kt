package test

import org.jetbrains.annotations.NotNull

public open class InheritNullabilityGenericSubclassSimple : java.lang.Object() {
    public open fun foo(): MutableCollection<String> = throw UnsupportedOperationException()

    public open class Sub: InheritNullabilityGenericSubclassSimple() {
        override fun foo(): MutableList<String> = throw UnsupportedOperationException()
    }
}
