annotation class My(
    public val x: Int,
    protected val y: Int,
    internal val z: Int,
    private val w: Int
)

open class Your {
    open val x: Int = 0
}

annotation class His(override val x: Int): Your()