interface AbstractFirTreeBuilder

class FirTreeBuilder : AbstractFirTreeBuilder

abstract class AbstractBuilderConfigurator<T : AbstractFirTreeBuilder> {
    abstract class BuilderConfigurationContext

    inner class LeafBuilderConfigurationContext : BuilderConfigurationContext()
}

class BuilderConfigurator : AbstractBuilderConfigurator<FirTreeBuilder>() {
    fun test(func: (LeafBuilderConfigurationContext) -> Unit) {
        val context = LeafBuilderConfigurationContext()
        func(context)
    }
}

class Outer<E> {
    inner class Inner

    fun foo(x: (Inner) -> Unit, y: Inner.() -> Unit) {
        // each call reported as INAPPLICABLE because "Inner<E> is not a subtype of Inner"
        bar(Inner())
        x(Inner())
        Inner().y()
    }

    fun bar(i: Inner) {}
}