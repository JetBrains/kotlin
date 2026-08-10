// LANGUAGE: +StrictEquals

interface Base {
    override fun equals(@EqualityBound(Base::class) other: Any?): Boolean
}

class Inherited : Base {
    override fun equals(other: Any?): Boolean = true
}

class Generic<T> {
    override fun equals(@EqualityBound(Generic::class) other: Any?): Boolean = true
}

typealias BaseAlias = Base

class Aliased : Base {
    override fun equals(@EqualityBound(BaseAlias::class) other: Any?): Boolean = true
}

data class Data(val value: Int)
