open class Test {
    var publicProperty: String = ""
        private set

    protected var protectedProperty: String = ""
        private set

    internal var internalProperty: String = ""
        private set

    fun update(i: Int) {
        publicProperty = i.toString()
        protectedProperty = i.toString()
        internalProperty = i.toString()
    }
}
