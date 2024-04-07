// ISSUE: KT-65408

data object CreateBuilder : Builder<CreateBuilder> {
    fun foo(): CreateBuilder = copy() // should be an error
}

interface Builder<T> : Cloneable
