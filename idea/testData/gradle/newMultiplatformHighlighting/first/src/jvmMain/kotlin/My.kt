actual class My {
    actual fun foo() {}

    actual fun <error descr="[ACTUAL_WITHOUT_EXPECT] Actual function 'bar' has no corresponding expected declaration">bar</error>() {}
}