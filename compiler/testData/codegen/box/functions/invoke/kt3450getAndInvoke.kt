//KT-3450 get and invoke are not parsed in one expression

public class A(val s: String) {

    public fun get(i: Int) : A = A("$s + $i")

    public fun invoke(builder : A.() -> String): String = builder()
}
fun x(y : String) : A = A(y)

fun foo() = x("aaa")[42] { "$s!!" }

fun box() = if (foo() == "aaa + 42!!") "OK" else "fail"
