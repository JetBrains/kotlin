//KT-1248 Control visibility of overrides needed
package kt1248

interface ParseResult<out T> {
    public val success : Boolean
    public val value : T
}

class Success<T>(internal override val value : T) : ParseResult<T> {
    internal override val success : Boolean = true
}

