// IGNORE_FE10
// In K1 out is different: MyInterface<*>
interface MyInterface<T> {
    fun sam(): T
}

fun test() = MyInterface:<caret>:sam
