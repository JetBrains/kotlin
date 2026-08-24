class Wrapper<T>

val simple: Unresolved
    get() = null!!

val generic: Unresolved<String>
    get() = null!!

val qualified: some.unknown.Unresolved
    get() = null!!

val nested: Wrapper<Unresolved>
    get() = null!!
