inline fun callable(): Int {
    val obj = object : BaseType {
        override fun compute(): Int {
            return 40 + extraCompute()
        }
    }
    return obj.compute()
}
