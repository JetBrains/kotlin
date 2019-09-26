fun foo() {
   <caret>when (f1()) {
        true -> f2()
        else -> {
            f2()
        }
    }
}

fun f1() = true
fun f2() {}

// EXISTS: f1()