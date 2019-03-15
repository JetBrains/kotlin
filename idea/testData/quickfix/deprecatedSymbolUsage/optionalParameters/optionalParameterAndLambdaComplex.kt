// "Replace with 'addA(d(createDummy, dummyParam1, initDummy), dummyParam)'" "true"
// WITH_RUNTIME

typealias NewDummyRef<V> = (Any) -> V

inline fun <A : Any> Any.d(
    createDummy: NewDummyRef<A>,
    dummyParam1: Int = 0,
    dummyParam2: Int = 0,
    initDummy: A.() -> Unit = {}
) = createDummy(this).also(initDummy).also { dummyParam1 + dummyParam2 }

@Deprecated("Use d instead", ReplaceWith("addA(d(createDummy, dummyParam1, initDummy), dummyParam)"))
inline fun <A : Any> MutableList<A>.addA(
    createDummy: NewDummyRef<A>,
    dummyParam1: Int = 0,
    dummyParam: Unit,
    initDummy: A.() -> Unit = {}
) = createDummy(this).also(initDummy).also { dummyParam1 }.also { add(it) }

@Deprecated("Use d instead", ReplaceWith("addA(d(createDummy, dummyParam1, dummyParam2, initDummy), dummyParam)"))
inline fun <A : Any> MutableList<A>.addA(
    createDummy: NewDummyRef<A>,
    dummyParam1: Int = 0,
    dummyParam2: Int = 0,
    dummyParam: Unit,
    initDummy: A.() -> Unit = {}
) = createDummy(this).also(initDummy).also { dummyParam1 + dummyParam2 }.also { add(it) }

@Suppress("NOTHING_TO_INLINE")
inline fun <A : Any> MutableList<A>.addA(a: A, dummyParam: Unit): A = a.also { add(a) }

fun createHi(any: Any) = "Hi $any"

val unDeprecateMe = mutableListOf("Hello").apply {
    addA<caret>(::createHi, 1, Unit) { // Run the quick fix from the IDE and watch it produce broken code.
        println("Yo")
    }
}