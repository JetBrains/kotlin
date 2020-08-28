typealias MyTypeAlias = (() -> String?)?
fun foo(x: MyTypeAlias) {

    x?.let { y -> y()?.let { result -> bar(result) } }
}

fun bar(x: String) = x
