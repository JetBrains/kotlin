// !DIAGNOSTICS: -UNUSED_PARAMETER
class A<T1, T2> {
    constructor(block: (T1) -> T2)
    constructor(x: T2): this({ x })
}
