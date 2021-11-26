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

@OptIn(ExperimentalTypeInference::class)
fun <R1 : Any> build2(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R1 : R2, R2 : Any> build3(@BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

@OptIn(ExperimentalTypeInference::class)
fun <R1 : R2, R2> build4(x: R2, @BuilderInference block: TestInterface<R1>.() -> Unit): R1 = TODO()

fun test(a: String?) {
    val ret1 = build {
        emit(1)
        get()?.equals("")
        val x = get()
        x?.equals("")
        x <!USELESS_ELVIS!>?: 1<!>
        x!!
        ""
    }
    val ret2 = build2 {
        emit(1)
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>get()<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")<!>
        val x = get()
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>x<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")<!>
        x <!USELESS_ELVIS!>?: 1<!>
        x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        ""
    }
    val ret3 = build3 {
        emit(1)
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>get()<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")<!>
        val x = get()
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>x<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")<!>
        x <!USELESS_ELVIS!>?: 1<!>
        x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        ""
    }
    val ret4 = build4(1) {
        emit(1)
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>get()<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")<!>
        val x = get()
        <!SAFE_CALL_WILL_CHANGE_NULLABILITY!>x<!UNNECESSARY_SAFE_CALL!>?.<!>equals("")<!>
        x <!USELESS_ELVIS!>?: 1<!>
        x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
        ""
    }
}
