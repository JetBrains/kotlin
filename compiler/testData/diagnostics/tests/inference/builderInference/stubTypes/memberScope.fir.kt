// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -DEPRECATION -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

fun Any.test() {}
fun Any?.test2() {}

fun test() {
    val ret1 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.<!NONE_APPLICABLE!>equals<!>(1)
        // there is `String?.equals` extension
        get().equals("")
    }
    val ret2 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.<!NONE_APPLICABLE!>equals<!>(1)
        val x = get()
        x?.hashCode()
        x?.<!NONE_APPLICABLE!>equals<!>(1)
        x.equals("")
    }
    val ret3 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.<!NONE_APPLICABLE!>equals<!>(1)
        val x = get()
        x?.hashCode()
        x?.<!NONE_APPLICABLE!>equals<!>(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x != null) {
            x<!UNNECESSARY_SAFE_CALL!>?.<!>hashCode()
            x<!UNNECESSARY_SAFE_CALL!>?.<!>equals(1)
            x.equals("")
            x.hashCode()
            x.toString()
            x.test()
            x<!UNNECESSARY_SAFE_CALL!>?.<!>test2()
            x.test2()
        }

        ""
    }
    val ret4 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x.hashCode()
        }

        ""
    }
    val ret401 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x.equals("")
        }

        ""
    }
    val ret402 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x<!UNSAFE_CALL!>.<!>toString("")
        }

        ""
    }
    val ret403 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x<!UNSAFE_CALL!>.<!>test()
        }

        ""
    }
    val ret404 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x.hashCode()
        }

        ""
    }
    val ret405 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x.equals("")
        }

        ""
    }
    val ret406 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x<!UNSAFE_CALL!>.<!>toString("")
        }

        ""
    }
    val ret407 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x<!UNSAFE_CALL!>.<!>test()
        }

        ""
    }
    val ret408 = build {
        emit(1)
        emit(null)
        val x = get()
        x.test()

        ""
    }
    val ret41 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.<!NONE_APPLICABLE!>equals<!>(1)
        val x = get()
        x?.hashCode()
        x?.<!NONE_APPLICABLE!>equals<!>(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x == null) {
            x?.hashCode()
        }

        if (x == null) {
            x?.<!NONE_APPLICABLE!>equals<!>(1)
        }

        if (x == null) {
            x?.test2()
        }

        if (x == null) {
            x.test2()
        }

        if (x === null) {
            x?.hashCode()
        }

        if (x === null) {
            x?.<!NONE_APPLICABLE!>equals<!>(1)
        }

        if (x === null) {
            x?.test2()
        }

        if (x === null) {
            x.test2()
        }

        ""
    }
    val ret5 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x.equals("")
        }

        ""
    }
    val ret501 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x.hashCode()
        }
        ""
    }
    val ret502 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x.toString()
        }
        ""
    }
    val ret503 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x<!UNSAFE_CALL!>.<!>test()
        }
        ""
    }
    val ret504 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x.equals("")
        }

        ""
    }
    val ret505 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x.hashCode()
        }
        ""
    }
    val ret506 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x.toString()
        }
        ""
    }
    val ret507 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x<!UNSAFE_CALL!>.<!>test()
        }
        ""
    }
    val ret508 = build {
        emit(1)
        emit(null)
        val x = get()
        x.test()
        ""
    }
    val ret51 = build {
        emit(1)
        emit(null)
        get()?.test()
        get()?.test2()
        get().test2()
        get()?.hashCode()
        get()?.<!NONE_APPLICABLE!>equals<!>(1)
        val x = get()
        x?.hashCode()
        x?.<!NONE_APPLICABLE!>equals<!>(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x == null) {
            x?.hashCode()
            x?.<!NONE_APPLICABLE!>equals<!>(1)
            x?.test2()
            x.test2()
        }

        ""
    }
}
