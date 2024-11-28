// FIR_IDENTICAL
// ISSUE: KT-72832

interface IParent<T,V> {
    fun b(t:T): V;
}

open class IParentImpl: IParent<String, String> {
    override fun b(t:String) = "ParentIntImpl"
}

interface IChild: IParent<String, String> {
}

open class Foo : IParentImpl(), IChild {
    override fun b(t:String) = "Foo"
}

open class Bar: IParentImpl(), IChild {
    override fun b(t:String) = "Bar"
}

fun getChild(): IChild? {
    val child: IChild? = Bar()

    val isFooOrBar = child is Foo || child is Bar

    // child is smartcasted to IParent<String, String>
    return if (isFooOrBar) child else null
}

fun test(): String? {
    return getChild()?.b("")
}
