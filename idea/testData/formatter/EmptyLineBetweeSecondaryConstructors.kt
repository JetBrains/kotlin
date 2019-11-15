class Some(b: Boolean) {
    // Comment.
    constructor(b: Int) : this(b == 0)
    /**
     * test
     * 2
     */
    constructor(b: String) : this(b.isEmpty())
    constructor(b: Long) : this(b == 0L)
}