trait A {
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

//internal trait A defined in root package
//internal abstract val v: jet.Int defined in A
//public abstract var int: jet.Int defined in A
//private abstract fun <set-int>(<set-?>: jet.Int): jet.Unit defined in A
//protected abstract fun f(): jet.Int defined in A
//internal final class B : A defined in root package
//public constructor B() defined in B
//public override var int: jet.Int defined in B
//public override fun <get-int>(): jet.Int defined in B
//private override fun <set-int>(<set-?>: jet.Int): jet.Unit defined in B
//protected override fun f(): jet.Int defined in B
//internal override val v: jet.Int defined in B
//internal override fun <get-v>(): jet.Int defined in B
