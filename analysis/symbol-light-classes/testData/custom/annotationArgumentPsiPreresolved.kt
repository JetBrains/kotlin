open class TopLevelClass {
    @Anno(value = MY_CONST)
    fun method() {}

    companion object {
        const val MY_CONST = "my text"
    }
}

annotation class Anno(val value: String)
