actual class <lineMarker descr="Has declaration in common module">My</lineMarker> {
    actual fun <lineMarker descr="Has declaration in common module">foo</lineMarker>() {}

    actual fun <error descr="[ACTUAL_WITHOUT_EXPECT] Actual function 'bar' has no corresponding expected declaration" textAttributesKey="ERRORS_ATTRIBUTES">bar</error>() {}
}