expect interface BaseInterface {
    fun select(x: Any): String
}

expect object ImplementBase : BaseInterface

class DelegatedClass(delegate: BaseInterface) : BaseInterface by delegate
