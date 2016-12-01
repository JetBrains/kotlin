fun main(args : Array<String>) {
    println(239.hashCode())
    println((-1L).hashCode())
    println('a'.hashCode())
    println(1.0f.hashCode())
    println(1.0.hashCode())
    println(true.hashCode())
    println(false.hashCode())
    println(Any().hashCode() != Any().hashCode())
    val a = CharArray(5)
    a[0] = 'H'
    a[1] = 'e'
    a[2] = 'l'
    a[3] = 'l'
    a[4] = 'o'
    // Note that it uses private Konan API.
    println("Hello".hashCode() == fromCharArray(a, 0, 5).hashCode())
}