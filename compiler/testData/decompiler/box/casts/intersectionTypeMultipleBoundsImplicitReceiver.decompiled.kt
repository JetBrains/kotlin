interface FirstTrait {
}
interface SecondTrait {
}
fun <T  : FirstTrait, SecondTrait> T.doSomething() : String  {
    return "OK"
}

class Foo: FirstTrait, SecondTrait {
    fun bar() : String  {
        return this.doSomething<Foo>()
    }

}
fun box() : String  {
    return Foo().bar()
}
