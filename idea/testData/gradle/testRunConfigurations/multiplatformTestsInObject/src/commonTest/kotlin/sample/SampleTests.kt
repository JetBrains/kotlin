package sample

import kotlin.test.Test
import kotlin.test.assertTrue

object <lineMarker descr="Run Test" settings="Nothing here">Obj</lineMarker> {
    @Test
    fun <lineMarker descr="Run Test" settings=" cleanJsBrowserTest jsBrowserTest --tests \"sample.Obj.myTest\" cleanJsNodeTest jsNodeTest --tests \"sample.Obj.myTest\" cleanJvmTest jvmTest --tests \"sample.Obj.myTest\" --continue">myTest</lineMarker>() {}
}