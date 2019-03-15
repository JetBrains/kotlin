class Inv<T>

inline operator fun <reified T> Inv<T>.invoke() {}

operator fun <K> Inv<K>.get(<!UNUSED_PARAMETER!>i<!>: Int): Inv<K> = this

fun <K> test(a: Inv<K>) {
    <!TYPE_PARAMETER_AS_REIFIED!>a[1]()<!>
}