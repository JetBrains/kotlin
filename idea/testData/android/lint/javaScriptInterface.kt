// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintJavascriptInterfaceInspection

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "UNUSED_VALUE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class JavaScriptTestK {
    fun test(webview: WebView) {
        webview.addJavascriptInterface(AnnotatedObject(), "myobj")

        webview.addJavascriptInterface(InheritsFromAnnotated(), "myobj")
        webview.addJavascriptInterface(NonAnnotatedObject(), "myobj")

        webview.addJavascriptInterface(null, "nothing")
        webview.addJavascriptInterface(object : Any() { @JavascriptInterface fun method() {} }, "nothing")
        webview.addJavascriptInterface(JavascriptFace(), "nothing")

        var o: Any = NonAnnotatedObject()
        webview.addJavascriptInterface(o, "myobj")
        o = InheritsFromAnnotated()
        webview.addJavascriptInterface(o, "myobj")
    }

    fun test(webview: WebView, object1: AnnotatedObject, object2: NonAnnotatedObject) {
        webview.addJavascriptInterface(object1, "myobj")
        webview.addJavascriptInterface(object2, "myobj")
    }

    @SuppressLint("JavascriptInterface")
    fun testSuppressed(webview: WebView) {
        webview.addJavascriptInterface(NonAnnotatedObject(), "myobj")
    }

    fun testLaterReassignment(webview: WebView) {
        var o: Any = NonAnnotatedObject()
        val t = o
        webview.addJavascriptInterface(t, "myobj")
        o = AnnotatedObject()
    }

    class NonAnnotatedObject() {
        fun test1() {}
        fun test2() {}
    }

    open class AnnotatedObject {
        @JavascriptInterface
        open fun test1() {}

        open fun test2() {}

        @JavascriptInterface
        fun test3() {}
    }

    class InheritsFromAnnotated : AnnotatedObject() {
        override fun test1() {}
        override fun test2() {}
    }

}

class JavascriptFace {
    fun method() {}
}