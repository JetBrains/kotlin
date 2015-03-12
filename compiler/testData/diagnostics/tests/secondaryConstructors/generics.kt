// !DIAGNOSTICS: -UNUSED_PARAMETER
open class B<T>(x: T, y: T) {
    constructor(x: T): this(x, x) {}
    constructor(): this(null) {}
}

class A0 : B<String?> {
    constructor() {}
    constructor(x: String): super(x) {}
    constructor(x: String, y: String): super(x, y) {}
}

class A1<R> : B<R> {
    constructor() {}
    constructor(x: R): super(x) {}
    constructor(x: R, y: R): super(x, y) {}
}

