// WITH_STDLIB
// Check that methods are generated if class is compiled in "disable" mode, regardless of jvmDefaultMode for implemented interfaces

// MODULE: libdisable
// JVM_DEFAULT_MODE: disable
// FILE: libdisable.kt

interface A<T> {
    fun f(t: T): T = t
}

// MODULE: libenable
// JVM_DEFAULT_MODE: enable
// FILE: libenable.kt

interface B<T> {
    fun m(t: T): T = t
}

@JvmDefaultWithoutCompatibility
interface C<T> {
    fun n(t: T): T = t
}

// MODULE: main(libenable, libdisable)
// JVM_DEFAULT_MODE: disable
// FILE: main.kt

class Test : A<String>, B<String>, C<String>
