trait <lineMarker descr="*"></lineMarker>SkipSupport {
    fun <lineMarker descr="<html><body>Is implemented in <br>&nbsp;&nbsp;&nbsp;&nbsp;SkipSupportWithDefaults</body></html>"></lineMarker>skip()
}

public trait <lineMarker descr="*"></lineMarker>SkipSupportWithDefaults : SkipSupport {
    override fun <lineMarker descr="Implements function in 'SkipSupport'"></lineMarker>skip() {}
}

open class SkipSupportImpl : SkipSupportWithDefaults