actual class <lineMarker>My</lineMarker> {
    actual fun <lineMarker>foo</lineMarker>() {}

    actual fun <error descr="[ACTUAL_WITHOUT_EXPECT] Actual function 'bar' has no corresponding expected declaration">bar</error>() {}
}