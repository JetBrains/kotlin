// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// KT-10444 Do not ignore smart (unchecked) casts to the same classifier

class Qwe<T : Any>(val a: T?) {
    fun test1(obj: Any) {
        obj as Qwe<T>
        check(obj.a)
    }

    fun test1(obj: Qwe<*>) {
        obj as Qwe<T>
        check(obj.a)
    }

    fun check(a: T?) {
    }
}

open class Foo
open class Bar<T: Foo>(open val a: T?, open val b: T?) {
    @Suppress("UNCHECKED_CAST")
    fun compare(obj: Any) {
        if (obj !is Bar<*>) {
            throw IllegalArgumentException()
        }
        if (System.currentTimeMillis() > 100) {
            val b = (obj as Bar<T>).b
            if (b == null) throw IllegalArgumentException()
            check(obj.a, b)
        }
    }
    fun check(a: T?, b: T) {
    }
}
