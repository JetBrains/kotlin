package androidx.compose.plugins.kotlin.frames

import androidx.compose.plugins.kotlin.AbstractComposeDiagnosticsTest

class FrameDiagnosticTests : AbstractComposeDiagnosticsTest() {

    // Ensure the simple case does not report an error
    fun testModel_Accept_Simple() = doTest(
        """
        import androidx.compose.Model

        @Model
        class MyModel {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on an open class
    fun testModel_Report_Open() = doTest(
        """
        import androidx.compose.Model

        @Model
        open class <!OPEN_MODEL!>MyModel<!> {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on an abstract class
    fun testModel_Report_Abstract() = doTest(
        """
        import androidx.compose.Model

        @Model
        abstract class <!OPEN_MODEL!>MyModel<!> {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on a class that specifies a base class
    fun testModel_Report_Inheritance() = doTest(
        """
        import androidx.compose.Model

        open class NonModel { }

        @Model
        class <!UNSUPPORTED_MODEL_INHERITANCE!>MyModel<!> : NonModel() {
          var strValue = "default"
        }
        """
    )
}