context(
fun foo() {
}

context(
val foo get() = ""

context(
val foo: String get() = ""

context(
var foo get() = ""

context(
var foo: String get() = ""

context(
class C {
    context(
    constructor() {} // no proper recovery because constructor is a soft keyword :(
}

context(c: String, : Int)
fun foo() {}

context(c: String : Int)
fun foo() {}