package androidx.compose.plugins.kotlin.frames

import androidx.compose.plugins.kotlin.AbstractComposeDiagnosticsTest

class FrameDiagnosticTests : AbstractComposeDiagnosticsTest() {

    // Ensure the simple case does not report an error
    fun testModel_Accept_Simple() = doTest(
        """
        import androidx.compose.<!DEPRECATION!>Model<!>

        @<!DEPRECATION!>Model<!>
        class MyModel {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on an open class
    fun testModel_Report_Open() = doTest(
        """
        import androidx.compose.<!DEPRECATION!>Model<!>

        @<!DEPRECATION!>Model<!>
        open class <!OPEN_MODEL!>MyModel<!> {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on an abstract class
    fun testModel_Report_Abstract() = doTest(
        """
        import androidx.compose.<!DEPRECATION!>Model<!>

        @<!DEPRECATION!>Model<!>
        abstract class <!OPEN_MODEL!>MyModel<!> {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model supports inheriting from a non-model class
    fun testModel_Report_Inheritance() = doTest(
        """
        import androidx.compose.<!DEPRECATION!>Model<!>

        open class NonModel { }

        @<!DEPRECATION!>Model<!>
        class MyModel : NonModel() {
          var strValue = "default"
        }
        """
    )

    // Ensure errors are reported when the class is nested.
    fun testModel_Report_Nested_Inheritance() = doTest(
        """
        import androidx.compose.<!DEPRECATION!>Model<!>

        open class NonModel { }

        class Tests {
            @<!DEPRECATION!>Model<!>
            open class <!OPEN_MODEL!>MyModel<!> : NonModel() {
              var strValue = "default"
            }
        }
        """
    )
}