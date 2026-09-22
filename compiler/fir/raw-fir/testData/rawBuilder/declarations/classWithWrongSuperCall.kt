open class A(init: A.() -> Unit) {
    val prop: String = ""
}

object B : A({})

object C : A(
    {
        fun foo() = B.prop.toString()
    }
)

class D : A(
    {
        fun foo() = B.prop.toString()
    }
)

class E() : A(
    {
        fun foo() = B.prop.toString()
    }
)

class F : A(
    {
        fun foo() = B.prop.toString()
    }
) {
    constructor()
}

class G : A(
    {
        fun foo() = B.prop.toString()
    }
) {
    constructor() : super(
        {
            fun foo() = C.prop.toString()
        }
    )
}

class H : A {
    constructor() : super(
        {
            fun foo() = B.prop.toString()
        }
    )
}