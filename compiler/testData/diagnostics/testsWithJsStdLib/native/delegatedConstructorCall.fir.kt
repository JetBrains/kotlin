// !DIAGNOSTICS: -DEBUG_INFO_MISSING_UNRESOLVED

external open class Base(x: Int) {
    constructor(x: String) : this(23)

    constructor(x: String, y: String) : this("")
}

external open class Derived1() : Base(23) {
    constructor(x: Byte) : super(23)

    constructor(x: String) : super("")

    constructor(x: String, y: String) : super("")
}

external open class Derived2() : Base("")
