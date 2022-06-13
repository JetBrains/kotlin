fun box() =
    B().method()

public open class A(){
    public open fun method() : String  = "OK"
}

public class B(): A(){
    public override fun method() : String {
        return ({
          super.method()
        }).let { it() }
    }
}
