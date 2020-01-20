package sample

import kotlin.test.Test
import kotlin.test.assertTrue

expect class <lineMarker descr="Run Test" settings=" cleanJsBrowserTest jsBrowserTest --tests \"sample.SampleTests\" cleanJsNodeTest jsNodeTest --tests \"sample.SampleTests\" cleanJvmTest jvmTest --tests \"sample.SampleTests\" --continue"><lineMarker descr="Has actuals in JS, JVM">SampleTests</lineMarker></lineMarker> {
    @Test
    fun <lineMarker descr="Run Test" settings=" cleanJsBrowserTest jsBrowserTest --tests \"sample.SampleTests.testMe\" cleanJsNodeTest jsNodeTest --tests \"sample.SampleTests.testMe\" cleanJvmTest jvmTest --tests \"sample.SampleTests.testMe\" --continue"><lineMarker descr="Has actuals in JS, JVM">testMe</lineMarker></lineMarker>()
}