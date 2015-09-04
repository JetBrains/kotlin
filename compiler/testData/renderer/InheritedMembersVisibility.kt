interface A {
    val v: Int
    public var int: Int
        private set

    protected fun f(): Int
}

class B : A {
    override var int: Int = 0
        get set
    override fun f(): Int {
    }
    override val v: Int = 0
        get
}

//public interface A defined in root package
//public abstract val v: kotlin.Int defined in A
//public abstract var int: kotlin.Int defined in A
//private abstract fun <set-int>(<set-?>: kotlin.Int): kotlin.Unit defined in A
//protected abstract fun f(): kotlin.Int defined in A
//public final class B : A defined in root package
//public constructor B() defined in B
//public open var int: kotlin.Int defined in B
//public open fun <get-int>(): kotlin.Int defined in B
//private open fun <set-int>(<set-?>: kotlin.Int): kotlin.Unit defined in B
//protected open fun f(): kotlin.Int defined in B
//public open val v: kotlin.Int defined in B
//public open fun <get-v>(): kotlin.Int defined in B
