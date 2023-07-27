class G<T>

<!CONFLICTING_JVM_DECLARATIONS!>val <T> G<T>.foo: Int
    get() = 1<!>

<!CONFLICTING_JVM_DECLARATIONS!>val G<String>.foo: Int
    get() = 1<!>
