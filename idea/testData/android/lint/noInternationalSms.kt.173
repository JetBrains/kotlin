// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintUnlocalizedSmsInspection

import android.content.Context
import android.telephony.SmsManager

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class NonInternationalizedSmsDetectorTest {
    private fun sendLocalizedMessage(context: Context) {
        // Don't warn here
        val sms = SmsManager.getDefault()
        sms.sendTextMessage("+1234567890", null, null, null, null)
    }

    private fun sendAlternativeCountryPrefix(context: Context) {
        // Do warn here
        val sms = SmsManager.getDefault()
        sms.sendMultipartTextMessage("<warning descr="To make sure the SMS can be sent by all users, please start the SMS number with a + and a country code or restrict the code invocation to people in the country you are targeting.">001234567890</warning>", null, null, null, null)
    }
}