interface <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl1<br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>"></lineMarker>skip()
}

public interface <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    override fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportImpl1</body></html>"><lineMarker descr="Implements function in 'SkipSupport'"></lineMarker></lineMarker>skip() {}
}

public interface SkipSupportImpl1 : SkipSupportWithDefaults {
    override fun <lineMarker descr="Overrides function in 'SkipSupportWithDefaults'"></lineMarker>skip() {}
}

open class SkipSupportImpl : SkipSupportWithDefaults