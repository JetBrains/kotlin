// ISSUE: KT-65408

data object CreateBuilder : Builder<CreateBuilder> {
    fun foo(): CreateBuilder = <!UNRESOLVED_REFERENCE!>copy<!>() // should be an error
}

interface Builder<T> : Cloneable
