fun main(args: Array<String>?) {
    val y: Unit = Unit //do not compile
    A<Unit>()        //do not compile
    C<Unit>(Unit)      //do not compile
        //do not compile
    fff<Unit>(Unit)  //do not compile
    id<Unit>(y)  //do not compile
    fff<Unit>(id<Unit>(y)) == id<Unit>(foreach(arrayOfNulls<Int>(0) as Array<Int>,{ e : Int -> }))  //do not compile
}
class A<T>()

class C<T>(val value: T) {
    fun foo(): T = value
}

fun <T> fff(x: T) : T { return x }

fun <T> id(value: T): T = value

fun foreach(array: Array<Int>, action: (Int)-> Unit) {
    for (el in array) {
       action(el) //exception through compilation (see below)
    }
}

fun almostFilter(array: Array<Int>, action: (Int)-> Int) {
    for (el in array) {
       action(el)
    }
}

fun box() : String {
    val a = arrayOfNulls<Int>(3) as Array<Int>
    a[0] = 0
    a[1] = 1
    a[2] = 2
    foreach(a, { el : Int -> el })
    almostFilter(a, { el : Int -> el })
    main(null)
    return "OK"
}
