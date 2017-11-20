// !WITH_NEW_INFERENCE
package a

class MyList<T>(<!UNUSED_PARAMETER!>t<!>: T) {}

fun <T> getMyList(t: T)           : MyList<    T> = MyList(t)
fun <T> getMyListToWriteTo(t: T)  : MyList< in T> = MyList(t)
fun <T> getMyListToReadFrom(t: T) : MyList<out T> = MyList(t)

fun <T> useMyList     (<!UNUSED_PARAMETER!>l<!>: MyList<    T>, <!UNUSED_PARAMETER!>t<!>: T) {}
fun <T> writeToMyList (<!UNUSED_PARAMETER!>l<!>: MyList< in T>, <!UNUSED_PARAMETER!>t<!>: T) {}
fun <T> readFromMyList(<!UNUSED_PARAMETER!>l<!>: MyList<out T>, <!UNUSED_PARAMETER!>t<!>: T) {}

fun test1(int: Int, any: Any) {
    val a0 : MyList<Any> = getMyList(int)

    val a1 : MyList<Int> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>getMyList(any)<!>

    val a2 : MyList<out Any> = getMyList(int)

    val a3 : MyList<out Any> = getMyListToReadFrom(int)

    val a4 : MyList<in Int> = getMyList(any)

    val a5 : MyList<in Int> = getMyListToWriteTo(any)


    val a6 : MyList<in Any> = <!TYPE_MISMATCH!>getMyList<Int>(int)<!>
    val a7 : MyList<in Any> = getMyList(int)

    val a8 : MyList<in Any> = <!TYPE_MISMATCH!>getMyListToReadFrom<Int>(int)<!>
    val a9 : MyList<in Any> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>getMyListToReadFrom(int)<!>

    val a10 : MyList<out Int> = <!TYPE_MISMATCH!>getMyList<Any>(any)<!>
    val a11 : MyList<out Int> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>getMyList(any)<!>

    val a12 : MyList<out Int> = <!TYPE_MISMATCH!>getMyListToWriteTo<Any>(any)<!>
    val a13 : MyList<out Int> = <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>getMyListToWriteTo(any)<!>

    useMyList(getMyList(int), int)
    useMyList(getMyList(any), int)
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>useMyList<!>(getMyList(int), any)

    readFromMyList(getMyList(int), any)
    readFromMyList(getMyList(any), int)
    readFromMyList<Int>(<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>getMyList(any)<!>, int)

    readFromMyList<Int>(<!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>getMyListToReadFrom(any)<!>, int)
    readFromMyList(getMyListToReadFrom(any), int)

    readFromMyList(getMyListToReadFrom(int), any)


    writeToMyList(getMyList(any), int)
    writeToMyList<Any>(getMyList(int), any)
    writeToMyList(getMyList<Any>(int), any)
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>writeToMyList<!>(getMyList(int), any)

    writeToMyList(getMyListToWriteTo(any), int)
    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>writeToMyList<!>(getMyListToWriteTo(int), any)

    readFromMyList(getMyListToWriteTo(any), any)

    <!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>writeToMyList<!>(getMyListToReadFrom(any), any)

    use(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13)
}

fun use(vararg a: Any) = a