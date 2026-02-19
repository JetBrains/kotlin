// WITH_STDLIB

// Check that methods are generated if module with class is compiled in NO-COMPATIBILITY mode
// only if the interface it implements is compiled in DISABLED mode

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
// JVM_DEFAULT_MODE: no-compatibility
// FILE: main.kt

class Test : A<String>, B<String>, C<String>
