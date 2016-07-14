open class <lineMarker></lineMarker>C(
        open val <lineMarker descr="<html><body>Is overridden in <br/>&nbsp;&nbsp;&nbsp;&nbsp;D</body></html>"></lineMarker>s: String
) {

}


class D : C("") {
    override val <lineMarker></lineMarker>s: String get() = "q"
}
