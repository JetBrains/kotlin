annotation class Anno

class C {
    @Anno
    internal val property: Int get() = 0
}

@Anno
internal val property: Int get() = 0
