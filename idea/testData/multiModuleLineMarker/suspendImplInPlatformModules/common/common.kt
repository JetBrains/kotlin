interface <lineMarker>I</lineMarker> {
    suspend fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;KJs<br>&nbsp;&nbsp;&nbsp;&nbsp;KJvm</body></html>">foo</lineMarker>(s: String)
}

/*
LINEMARKER: <html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;KJs<br>&nbsp;&nbsp;&nbsp;&nbsp;KJvm</body></html>
TARGETS:
js.kt
    suspend override fun <1>foo(s: String) {
 jvm.kt
    suspend override fun <2>foo(s: String) {
*/
