package b

public open class B {
    public var OK: String = "OK"
        protected set
}

public class BB : B() {
    public fun ok(): String = OK
}