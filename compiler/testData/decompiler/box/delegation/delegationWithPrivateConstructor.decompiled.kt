class MyObject(val delegate: Interface): Interface by delegate {
    constructor() : this(Delegate()), Interface {
    }
}
class Delegate: Interface {
    override fun greet() : String  {
        return "OK"
    }

}
private interface Interface {
    abstract  fun greet() : String
}
fun box() : String  {
    return MyObject().greet()
}
