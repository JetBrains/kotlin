class Some(classNames: () -> Collection<String>) {
    internal val first by lazy {
        classNames().toSet()
    }

    private val second by lazy {
        val nonDeclaredNames = getNonDeclaredClassifierNames() ?: return@lazy null
        val allNames = first + nonDeclaredNames
        allNames
    }

    fun getNonDeclaredClassifierNames(): Set<String>? = null
}