class G<T>

val <T> G<T>.foo: Int
    <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1

val G<String>.foo: Int
    <!CONFLICTING_JVM_DECLARATIONS!>get()<!> = 1