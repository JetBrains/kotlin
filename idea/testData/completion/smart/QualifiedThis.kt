class Foo{
    fun String.foo(){
        val foo : Foo = <caret>
    }
}

// EXIST: { lookupString:"this@Foo", typeText:"Foo" }
