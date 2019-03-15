// !WITH_NEW_INFERENCE
package h

interface A<T> {}

fun <T> newA(): A<T> = throw Exception()

interface Z

fun <T> id(t: T): T = t

//binary expressions
//identifier
infix fun <T> Z.foo(a: A<T>): A<T> = a

fun test(z: Z) {
    z <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>foo<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>()
    val a: A<Int> = id(z foo newA())
    val b: A<Int> = id(z.foo(newA()))
    use(a, b)
}

//binary operation expression
operator fun <T> Z.plus(a: A<T>): A<T> = a

fun test1(z: Z) {
    id(z <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>+<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>())
    <!NI;UNREACHABLE_CODE!>val a: A<Z> = z + newA()<!>
    <!NI;UNREACHABLE_CODE!>val b: A<Z> = z.plus(newA())<!>
    <!NI;UNREACHABLE_CODE!>val c: A<Z> = id(z + newA())<!>
    <!NI;UNREACHABLE_CODE!>val d: A<Z> = id(z.plus(newA()))<!>
    <!NI;UNREACHABLE_CODE!>use(a, b, c, d)<!>
}

//comparison operation
operator fun <T> Z.compareTo(a: A<T>): Int { use(a); return 1 }

fun test2(z: Z) {
    val a: Boolean = id(z <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!><<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>())
    val b: Boolean = id(z < newA<Z>())
    use(a, b)
}

//'equals' operation
fun Z.<!EXTENSION_SHADOWED_BY_MEMBER!>equals<!>(any: Any): Int { use(any); return 1 }

fun test3(z: Z) {
    z <!NI;EQUALS_MISSING!>==<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>()
    z == newA<Z>()
    id(z <!NI;EQUALS_MISSING!>==<!> <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>())
    id(z == newA<Z>())

    id(z === <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>())
    id(z === newA<Z>())
}

//'in' operation
fun test4(collection: Collection<A<*>>) {
    id(<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>newA<!>() in collection)
    <!NI;UNREACHABLE_CODE!>id(newA<Int>() in collection)<!>
}

//boolean operations
fun <T> toBeOrNot(): Boolean = throw Exception()

fun test5() {
    if (<!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>toBeOrNot<!>() && <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>toBeOrNot<!>()) {}
    if (toBeOrNot<Int>() && toBeOrNot<Int>()) {}
}

//use
fun use(vararg a: Any?) = a