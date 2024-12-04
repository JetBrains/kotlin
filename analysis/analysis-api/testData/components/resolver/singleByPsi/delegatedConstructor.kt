open class A(x: Int) {
    protected constructor() : this(1) {}
    private constructor(p: String) : this(2) {}
}

class B(): <expr>A</expr>(5)