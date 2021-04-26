// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun use(a: Any?) = a

fun test() {
    { }<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>
    use({ }<!NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION!>!!<!>);

    // KT-KT-9070
    { } <!USELESS_ELVIS!>?: 1<!>
    use({ 2 } <!USELESS_ELVIS!>?: 1<!>);

    1 <!USELESS_ELVIS!>?: { }<!>
    use(1 <!USELESS_ELVIS!>?: { }<!>)
}
