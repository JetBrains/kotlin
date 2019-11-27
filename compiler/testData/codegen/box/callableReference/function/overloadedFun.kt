// IGNORE_BACKEND_FIR: JVM_IR
fun foo(): String = "foo1"
fun foo(i: Int): String = "foo2"

val f1: () -> String = ::foo
val f2: (Int) -> String = ::foo

fun foo1() {}
fun foo2(i: Int) {}

fun bar(f: () -> Unit): String = "bar1"
fun bar(f: (Int) -> Unit): String = "bar2"

fun box(): String {
    val x1 = f1()
    if (x1 != "foo1") return "Fail 1: $x1"
    
    val x2 = f2(0)
    if (x2 != "foo2") return "Fail 2: $x2"
    
    val y1 = bar(::foo1)
    if (y1 != "bar1") return "Fail 3: $y1"
    
    val y2 = bar(::foo2)
    if (y2 != "bar2") return "Fail 4: $y2"
    
    return "OK"
}
