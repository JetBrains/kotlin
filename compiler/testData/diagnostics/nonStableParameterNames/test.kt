// !WITH_NEW_INFERENCE
// !LANGUAGE: +NewInference
// MODULE: m1-common
// FILE: test.kt

import library.*

fun test() {
    foo(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true
    )

    Foo(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true
    ).foo(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true
    )

    Foo(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true,
        3.14
    )

    Foo.Bar(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true
    ).bar(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true
    )

    Foo.Bar(
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>a<!> = 42,
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>b<!> = "hello",
        <!NAMED_ARGUMENTS_NOT_ALLOWED!>c<!> = true,
        3.14
    )
}
