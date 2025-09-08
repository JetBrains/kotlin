open class A<T>(i: T) {
    constructor(i: T, str: String) : this((((i))))
    constructor(i: T, value: Int) : this(i = (((i))))
}

class B : A<(((Int)))> {
    constructor() : super(((1)))

    constructor(value: Int) : super(i = ((value)))

    constructor(value: String) : super(i = ((5)) as Int)
}