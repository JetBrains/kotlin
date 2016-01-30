
public fun <warning descr="Missing documentation">publicUndocumentedFun</warning>() {}
fun <warning descr="Missing documentation">defaultUndocumentedFun</warning>() {}

/** Some documentation */
public fun publicDocumentedFun() {}

/** Some documentation */
fun defaultDocumentedFun() {}

private fun privateUndocumentedFun() {}
internal fun internalUndocumentedFun() {}



public class <warning descr="Missing documentation">publicUndocumentedClass</warning>() {}
class <warning descr="Missing documentation">defaultUndocumentedClass</warning>() {}

/** Some documentation */
public class publicDocumentedClass() {}

/** Some documentation */
class defaultDocumentedClass() {}

private class privateUndocumentedClass() {}
internal class internalUndocumentedClass() {}



private open class Properties {

    public open val <warning descr="Missing documentation">publicUndocumentedProperty</warning>: Int = 0
    open val <warning descr="Missing documentation">defaultUndocumentedProperty</warning>: Int = 0

    /** Some documentation */
    public open val publicDocumentedProperty: Int = 0

    /** Some documentation */
    open val defaultDocumentedProperty: Int = 0

    private val privateUndocumentedProperty: Int = 0
    internal open val internalUndocumentedProperty: Int = 0


    protected open val protectedUndocumentedProperty: Int = 0
    protected class protectedUndocumentedClass {}
    protected fun protectedUndocumentedFun() {}

    /** Some documentation */
    protected open val protectedDocumentedProperty: Int = 0
}

private open class ChildClass : Properties() {
    override val <warning descr="Missing documentation">publicUndocumentedProperty</warning>: Int = 4
    override val <warning descr="Missing documentation">defaultUndocumentedProperty</warning>: Int = 4
    override val publicDocumentedProperty: Int = 4
    override val defaultDocumentedProperty: Int = 4

    /** Some documentation */
    override public val internalUndocumentedProperty: Int = 4
    override public val <warning descr="Missing documentation">protectedUndocumentedProperty</warning>: Int = 4
    override public val protectedDocumentedProperty: Int = 4
}

private class GrandChildClass : ChildClass() {
    override public val internalUndocumentedProperty: Int = 6
}

// NO_CHECK_INFOS
