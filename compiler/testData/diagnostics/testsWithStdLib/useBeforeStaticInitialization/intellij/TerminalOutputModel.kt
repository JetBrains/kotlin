// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// WITH_STDLIB
sealed interface TerminalOffset : Comparable<TerminalOffset> {
    companion object {
        /**
         * Creates a new offset instance with the given value.
         */
        @JvmStatic
        fun of(absoluteOffset: Long): TerminalOffset = TerminalOffsetImpl(absoluteOffset)

        /**
         * The offset of the beginning of the output history.
         */
        @JvmField
        val ZERO: TerminalOffset = of(0L)
    }

    /**
     * Returns the absolute offset from the beginning of the output history.
     */
    fun toAbsolute(): Long

    /**
     * Adds the given value to the offset and returns the new offset.
     */
    operator fun plus(charCount: Long): TerminalOffset

    /**
     * Subtracts the given value from the offset and returns the new offset.
     */
    operator fun minus(charCount: Long): TerminalOffset

    /**
     * Calculates the difference between this offset and the other offset.
     */
    operator fun minus(other: TerminalOffset): Long
}

sealed interface TerminalLineIndex : Comparable<TerminalLineIndex> {
    companion object {
        /**
         * Creates a new line index instance with the given value.
         */
        @JvmStatic fun of(absoluteOffset: Long): TerminalLineIndex = TerminalLineIndexImpl(absoluteOffset)

        /**
         * The line index of the beginning of the output history.
         */
        @JvmField val ZERO: TerminalLineIndex = of(0L)
    }

    /**
     * Returns the absolute line index from the beginning of the output history.
     */
    fun toAbsolute(): Long

    /**
     * Adds the given value to the line index and returns the new line index.
     */
    operator fun plus(lineCount: Long): TerminalLineIndex

    /**
     * Subtracts the given value from the line index and returns the new line index.
     */
    operator fun minus(lineCount: Long): TerminalLineIndex

    /**
     * Calculates the difference between this line index and the other line index.
     */
    operator fun minus(other: TerminalLineIndex): Long
}


private data class TerminalOffsetImpl(private val absolute: Long) : TerminalOffset {
    override fun compareTo(other: TerminalOffset): Int = toAbsolute().compareTo(other.toAbsolute())
    override fun toAbsolute(): Long = absolute
    override fun plus(charCount: Long): TerminalOffset = TerminalOffsetImpl(absolute + charCount)
    override fun minus(charCount: Long): TerminalOffset = plus(-charCount)
    override fun minus(other: TerminalOffset): Long = toAbsolute() - other.toAbsolute()
    override fun toString(): String = "${toAbsolute()}L"
}

private data class TerminalLineIndexImpl(private val absolute: Long) : TerminalLineIndex {
    override fun compareTo(other: TerminalLineIndex): Int = toAbsolute().compareTo(other.toAbsolute())
    override fun toAbsolute(): Long = absolute
    override fun plus(lineCount: Long): TerminalLineIndex = TerminalLineIndexImpl(absolute + lineCount)
    override fun minus(lineCount: Long): TerminalLineIndex = TerminalLineIndexImpl(absolute - lineCount)
    override fun minus(other: TerminalLineIndex): Long = toAbsolute() - other.toAbsolute()
    override fun toString(): String = "${toAbsolute()}L"
}

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, companionObject, data, functionDeclaration,
interfaceDeclaration, objectDeclaration, operator, override, primaryConstructor, propertyDeclaration, sealed,
stringLiteral, unaryExpression */
