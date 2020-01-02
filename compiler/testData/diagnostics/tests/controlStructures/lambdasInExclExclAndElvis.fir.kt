// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_EXPRESSION

fun use(a: Any?) = a

fun test() {
    { }!!
    use({ }!!);

    // KT-KT-9070
    { } ?: 1
    use({ 2 } ?: 1);

    1 ?: { }
    use(1 ?: { })
}