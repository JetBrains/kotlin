
public fun <warning descr="Missing Documentation">publicUndocumentedFun</warning>() {}
fun <warning descr="Missing Documentation">defaultUndocumentedFun</warning>() {}

/** Some documentation */
public fun publicDocumentedFun() {}

/** Some documentation */
fun defaultDocumentedFun() {}

private fun privateUndocumentedFun() {}
internal fun internalUndocumentedFun() {}



public class <warning descr="Missing Documentation">publicUndocumentedClass</warning>() {}
class <warning descr="Missing Documentation">defaultUndocumentedClass</warning>() {}

/** Some documentation */
public class publicDocumentedClass() {}

/** Some documentation */
class defaultDocumentedClass() {}

private class privateUndocumentedClass() {}
internal class internalUndocumentedClass() {}



private class Properties {

    public val <warning descr="Missing Documentation">publicUndocumentedProperty</warning>: Int = 0
    val <warning descr="Missing Documentation">defaultUndocumentedProperty</warning>: Int = 0

    /** Some documentation */
    public val publicDocumentedProperty: Int = 0

    /** Some documentation */
    val defaultDocumentedProperty: Int = 0

    private val privateUndocumentedProperty: Int = 0
    internal val internalUndocumentedProperty: Int = 0
    protected val protectedUndocumentedProperty: Int = 0
}

// NO_CHECK_INFOS
