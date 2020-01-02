//KT-1358 Overload resolution ambiguity with smartcast and generic function
package d

fun bar(a: Any?) {
    if (a != null) {
        a.foo() //overload resolution ambiguity
        a.sure() //overload resolution ambiguity
    }
}

fun <T : Any> T?.foo() {}
fun <T : Any> T?.sure() : T = this!!