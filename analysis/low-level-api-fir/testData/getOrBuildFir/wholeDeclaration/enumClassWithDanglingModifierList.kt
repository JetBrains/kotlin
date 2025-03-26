<expr>enum class MyEnumClass {
    Entry {
        @Anno("enum entry dangling modifier")
    };

    fun regularMember(i: Int) = 25

    @Anno("enum dangling modifier")
}</expr>

annotation class Anno(val value: String)
