
public fun <warning descr="Function \"publicUndocumentedFun\" is missing documentation">publicUndocumentedFun</warning>() {}
fun <warning descr="Function \"defaultUndocumentedFun\" is missing documentation">defaultUndocumentedFun</warning>() {}

/** Some documentation */
public fun publicDocumentedFun() {}

/** Some documentation */
fun defaultDocumentedFun() {}

private fun privateUndocumentedFun() {}
internal fun internalUndocumentedFun() {}



public class <warning descr="Class \"publicUndocumentedClass\" is missing documentation">publicUndocumentedClass</warning>() {}
class <warning descr="Class \"defaultUndocumentedClass\" is missing documentation">defaultUndocumentedClass</warning>() {}

/** Some documentation */
public class publicDocumentedClass() {}

/** Some documentation */
class defaultDocumentedClass() {}

private class privateUndocumentedClass() {}
internal class internalUndocumentedClass() {}



private open class Properties {

    public open val publicUndocumentedProperty: Int = 0
    open val defaultUndocumentedProperty: Int = 0

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
    override val publicUndocumentedProperty: Int = 4
    override val defaultUndocumentedProperty: Int = 4
    override val publicDocumentedProperty: Int = 4
    override val defaultDocumentedProperty: Int = 4

    /** Some documentation */
    override public val internalUndocumentedProperty: Int = 4
    override public val protectedUndocumentedProperty: Int = 4
    override public val protectedDocumentedProperty: Int = 4
}

private class GrandChildClass : ChildClass() {
    override public val internalUndocumentedProperty: Int = 6
}

open class <warning descr="Class \"SomeClass\" is missing documentation">SomeClass</warning> {
    protected fun <warning descr="Function \"testProtected\" is missing documentation">testProtected</warning>() = 1
}

class <warning descr="Class \"FinalClassWithProtected\" is missing documentation">FinalClassWithProtected</warning> {
    protected fun <warning descr="Function \"testProtected\" is missing documentation">testProtected</warning>() = 1
}

private class PrimaryCon(val p: String)


// NO_CHECK_INFOS
