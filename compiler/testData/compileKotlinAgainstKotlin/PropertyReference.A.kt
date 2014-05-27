package a

public var topLevel: Int = 42

public val String.extension: Long
    get() = length.toLong()
