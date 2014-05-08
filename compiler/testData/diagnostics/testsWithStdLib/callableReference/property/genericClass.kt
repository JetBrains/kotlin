class A<T>(val t: T) {
    val foo: T = t
}

fun bar() {
    val x = A<String>::foo
    x : KMemberProperty<A<String>, String>
    x : KMemberProperty<A<String>, Any?>

    val y = A<*>::foo
    y : KMemberProperty<A<*>, Any?>
}
