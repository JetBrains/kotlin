fun test1(a) {}
fun test2(a = 4) {}
fun test3(c: Int) {}

fun test4(ann(parameter) a) {}
fun test5(ann a) {}

fun test() {
    try {

    }
    catch(a: Int) {

    }
}

val a = fun (b) {}
val a = fun (b = 4) {}
val a = fun (b: Int) {}

val a: (A) -> Unit
val a: (a: A) -> Unit

class A(a: Int)