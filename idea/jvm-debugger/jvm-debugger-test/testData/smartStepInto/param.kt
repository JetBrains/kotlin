fun foo() {
    f1(f2())<caret>
}

fun f1(i: Int) = 1
fun f2() {}

// EXISTS: f1(Int), f2()