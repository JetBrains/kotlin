// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun use(a: Any?) = a

fun test() {
    { }<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
    use({ }<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>);

    // KT-KT-9070
    <!OI;TYPE_MISMATCH!>{ }<!> <!USELESS_ELVIS!>?: 1<!>
    use({ 2 } <!USELESS_ELVIS!>?: 1<!>);

    1 <!USELESS_ELVIS!>?: <!OI;TYPE_MISMATCH, UNUSED_LAMBDA_EXPRESSION!>{ }<!><!>
    use(1 <!USELESS_ELVIS!>?: { }<!>)
}