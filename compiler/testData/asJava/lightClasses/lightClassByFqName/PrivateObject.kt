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
