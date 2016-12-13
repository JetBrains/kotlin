enum class SampleEnum {
    V1 {
        override fun <lineMarker descr="Overrides function in 'SampleEnum'">any</lineMarker>() { super.any() }
    };

    open fun <lineMarker descr="<html><body>Is overridden in <br>&nbsp;&nbsp;&nbsp;&nbsp;Enum constant 'V1' in 'SampleEnum'</body></html>">any</lineMarker>() {}
}
