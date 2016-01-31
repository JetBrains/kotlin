
public fun <warning descr="Missing Documentation">publicUndocumentedFun</warning>() {}

/** Some documentation */
public fun publicDocumentedFun() {}

fun defaultUndocumentedFun() {}
private fun privateUndocumentedFun() {}
internal fun internalUndocumentedFun() {}



public class <warning descr="Missing Documentation">publicUndocumentedClass</warning>() {}

/** Some documentation */
public class publicDocumentedClass() {}

class defaultUndocumentedClass() {}
private class privateUndocumentedClass() {}
internal class internalUndocumentedClass() {}



private class Properties {

    public val <warning descr="Missing Documentation">publicUndocumentedProperty</warning>: Int = 0

    /** Some documentation */
    public val publicDocumentedProperty: Int = 0

    val defaultUndocumentedProperty: Int = 0
    private val privateUndocumentedProperty: Int = 0
    internal val internalUndocumentedProperty: Int = 0
    protected val protectedUndocumentedProperty: Int = 0
}

// NO_CHECK_INFOS
