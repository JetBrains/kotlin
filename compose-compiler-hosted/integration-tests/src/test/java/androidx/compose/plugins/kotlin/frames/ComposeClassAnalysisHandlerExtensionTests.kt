package androidx.compose.plugins.kotlin.frames

import androidx.compose.plugins.kotlin.AbstractComposeDiagnosticsTest

class ComposeClassAnalysisHandlerExtensionTests : AbstractComposeDiagnosticsTest() {

    fun testReportOpen() {
        doTest("""
            import androidx.compose.Component;

            open class <!OPEN_COMPONENT!>MyComponent<!> : Component() {
               override fun compose() { }
            }
        """)
    }

    fun testAllowClosed() {
        doTest("""
            import androidx.compose.Component;

            class MyComponent: Component() {
               override fun compose() { }
            }
        """)
    }

    fun testReportAbstract() {
        doTest("""
            import androidx.compose.Component;

            abstract class <!OPEN_COMPONENT!>MyComponent<!>: Component() {
               override fun compose() { }
            }
        """)
    }
}
