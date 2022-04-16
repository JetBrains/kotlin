open class Base(v: String)

expect class Derived(v: String) : Base

expect open class ExpectBase(v: String)

expect class ExpectDerived(v: String) : ExpectBase

expect open class IOException(message: String, cause: Throwable?) {
    constructor(message: String)
}

expect class EOFException(message: String) : IOException
