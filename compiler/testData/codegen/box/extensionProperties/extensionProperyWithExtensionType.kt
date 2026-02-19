var <T> T.varProp: T.()-> String
    get() = { "1" }
    set(value: T.()-> String) {}

var Int.intProp: Int.(String)-> String
    get() = { y: String -> y }
    set(value: Int.(String) -> String) { }

fun test(){
    val funFromVarProp = 1.varProp
    funFromVarProp(1)
    1.funFromVarProp()
    1.varProp = { "2" }

    val funFromIntProp = 1.intProp
    funFromIntProp(1, "")
    1.funFromIntProp("")
    1.intProp = { y: String -> y }
}

fun box(): String {
    test()
    return "OK"
}