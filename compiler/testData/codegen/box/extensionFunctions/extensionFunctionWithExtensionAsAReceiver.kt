var result1 = "failed"
var result2 = "failed"

fun (Int.()-> String).foo(a: Int.()-> String){
    result1 = "O"
}

fun <T> (Int.()-> T).bar(a: Int.()-> T) {
    result2 = "K"
}

fun <T> test(): Int.() -> T {
    return { 1 as T }
}

fun box(): String {
    test<String>().foo(test())
    test<Int>().bar(test())
    return result1+result2
}