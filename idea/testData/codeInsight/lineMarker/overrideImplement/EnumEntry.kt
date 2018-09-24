enum class SampleEnum {
    V1 {
        override fun <lineMarker descr="Overrides function in 'SampleEnum'">any</lineMarker>() { super.any() }
    };

    open fun <lineMarker descr="<html><body>Is overridden in <br>&nbsp;&nbsp;&nbsp;&nbsp;SampleEnum.V1</body></html>">any</lineMarker>() {}
}
