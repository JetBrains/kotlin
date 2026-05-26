// one.PrivateObject
package one

private object PrivateObject {
    var publicProperty = 4
    internal var internalProperty = false
    private var privateProperty = ""

    fun publicFun() {}
    internal fun internalFun() {}
    private fun privateFun() {}
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: PrivateObject.class[internalFun]
// LIGHT_ELEMENTS_NO_DECLARATION: PrivateObject.class[INSTANCE;PrivateObject;getInternalProperty$main;internalFun$main;setInternalProperty$main]
