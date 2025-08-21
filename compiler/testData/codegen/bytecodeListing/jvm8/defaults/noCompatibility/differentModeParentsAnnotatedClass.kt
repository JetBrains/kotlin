// WITH_STDLIB
// IGNORE_BACKEND_K1: JVM_IR

// Check that methods are generated if module with class is compiled in NO-COMPATIBILITY mode,
// but the class iteself is annotated with @JvmDefaultWithCompatibility,
// for all jvmDefaultModes for implemented interfaces

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

@JvmDefaultWithCompatibility
class Test : A<String>, B<String>, C<String>
