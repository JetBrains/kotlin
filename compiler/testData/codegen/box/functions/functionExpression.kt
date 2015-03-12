val foo1 = fun Any.(): String {
return  "239" + this
}

val foo2 = fun Int.(i : Int) : Int = this + i

fun <T> fooT1() = fun (t : T) = t.toString()

annotation class A

fun box() : String {
    if(10.foo1() != "23910") return "foo1 fail"
    if(10.foo2(1) != 11) return "foo2 fail"

    if(1.(fun Int.() = this + 1)() != 2) return "test 3 failed";
    if(  (fun () = 1)() != 1)  return "test 4 failed";
    if(  (fun (i: Int) = i)(1) != 1)  return "test 5 failed";
    if(  1.(fun Int.(i: Int) = i + this)(1) != 2) return "test 6 failed";
    if(  (fooT1<String>()("mama")) != "mama")  return "test 7 failed";

    val a = [A] fun Int.() = this + 1 // 
    if (1.a() != 2) return "test 8 failed"
    val b = ( fun Int.() = this + 1)
    if (1.a() != 2) return "test 9 failed"
    val c = (@c fun Int.() = this + 1)
    if (1.a() != 2) return "test 10 failed"

    val d = @d fun (): Int { return@d 4}
    if (d() != 4) return "test 11 failed"

    return "OK"
}
