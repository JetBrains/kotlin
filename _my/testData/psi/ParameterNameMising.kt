fun foo(Array<String>) : Unit {
    var x : Int = 42
    x += 1
}

fun bar(Array<String> = array("")) : Unit {
    var x : Int = 42
    x += 1
}