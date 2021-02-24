// !WITH_NEW_INFERENCE
package a

class MyList<T>(t: T) {}

fun <T> getMyList(t: T)           : MyList<    T> = MyList(t)
fun <T> getMyListToWriteTo(t: T)  : MyList< in T> = MyList(t)
fun <T> getMyListToReadFrom(t: T) : MyList<out T> = MyList(t)

fun <T> useMyList     (l: MyList<    T>, t: T) {}
fun <T> writeToMyList (l: MyList< in T>, t: T) {}
fun <T> readFromMyList(l: MyList<out T>, t: T) {}

fun test1(int: Int, any: Any) {
    val a0 : MyList<Any> = getMyList(int)

    val a1 : MyList<Int> = getMyList(any)

    val a2 : MyList<out Any> = getMyList(int)

    val a3 : MyList<out Any> = getMyListToReadFrom(int)

    val a4 : MyList<in Int> = getMyList(any)

    val a5 : MyList<in Int> = getMyListToWriteTo(any)


    val a6 : MyList<in Any> = getMyList<Int>(int)
    val a7 : MyList<in Any> = getMyList(int)

    val a8 : MyList<in Any> = getMyListToReadFrom<Int>(int)
    val a9 : MyList<in Any> = getMyListToReadFrom(int)

    val a10 : MyList<out Int> = getMyList<Any>(any)
    val a11 : MyList<out Int> = getMyList(any)

    val a12 : MyList<out Int> = getMyListToWriteTo<Any>(any)
    val a13 : MyList<out Int> = getMyListToWriteTo(any)

    useMyList(getMyList(int), int)
    useMyList(getMyList(any), int)
    useMyList(getMyList(int), any)

    readFromMyList(getMyList(int), any)
    readFromMyList(getMyList(any), int)
    <!INAPPLICABLE_CANDIDATE!>readFromMyList<!><Int>(getMyList(any), int)

    <!INAPPLICABLE_CANDIDATE!>readFromMyList<!><Int>(getMyListToReadFrom(any), int)
    readFromMyList(getMyListToReadFrom(any), int)

    readFromMyList(getMyListToReadFrom(int), any)


    writeToMyList(getMyList(any), int)
    writeToMyList<Any>(getMyList(int), any)
    writeToMyList(getMyList<Any>(int), any)
    writeToMyList(getMyList(int), any)

    writeToMyList(getMyListToWriteTo(any), int)
    writeToMyList(getMyListToWriteTo(int), any)

    readFromMyList(getMyListToWriteTo(any), any)

    <!INAPPLICABLE_CANDIDATE!>writeToMyList<!>(getMyListToReadFrom(any), any)

    use(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
}

fun use(vararg a: Any) = a
