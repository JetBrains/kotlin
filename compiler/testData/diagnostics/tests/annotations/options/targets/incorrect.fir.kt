// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target(AnnotationTarget.<!UNRESOLVED_REFERENCE!>INIT<!>) annotation class incorrect

@incorrect annotation class derived

@incorrect class correct(@incorrect val x: Int, @incorrect w: @incorrect Int) {
    @incorrect constructor(): this(0, 0)
}

@incorrect enum class My @incorrect constructor() {
    @incorrect FIRST,
    @incorrect SECOND
}

@incorrect fun foo(@incorrect y: @incorrect Int): Int {
    @incorrect fun bar(@incorrect z: @incorrect Int) = z + 1
    @incorrect val local = bar(y)
    return local
}

@incorrect val z = @incorrect 0
