// RUN_PIPELINE_TILL: FRONTEND

// This file doesn't contain LANGUAGE directive because it's expected
// to work independently of language-version-settings configuration, so there should be no LATEST_LV_DIFFERENCE.

// FILE: Action.java
public interface Action<T> {
    void accept(T t);
}

// FILE: main.kt
fun foo(name: String = "", x: String.() -> Unit) {}
fun foo(x: Action<String>) {}

fun interface MyAction<E> {
    fun accept(e: E)
}

fun bar(name: String = "", x: String.() -> Unit) {}
fun bar(x: MyAction<String>) {}

fun main() {
    foo {
        length
    }

    foo("1") {
        length
    }

    bar {
        <!UNRESOLVED_REFERENCE!>length<!>
    }

    bar("") {
        length
    }
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
nullableType, samConversion, stringLiteral, suspend, typeParameter */
