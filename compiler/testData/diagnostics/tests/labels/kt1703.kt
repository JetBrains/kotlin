// !DIAGNOSTICS: -UNUSED_PARAMETER
public inline fun <reified T> Array(n: Int, block: (Int) -> T): Array<T> = null!!

//KT-1703 Reference to label is unresolved

fun test() {
    val ints = Array<Int?>(2, { null })
    ints.forEach @lit {
        if (it == null) <!RETURN_NOT_ALLOWED_EXPLICIT_RETURN_TYPE_REQUIRED!>return @lit<!>
        use(<!DEBUG_INFO_SMARTCAST!>it<!> + 5)
    }
}

fun <T> Array<out T>.forEach(operation: (T) -> Unit) {
    for (element in this) operation(element)
}

fun use(a: Any?) = a