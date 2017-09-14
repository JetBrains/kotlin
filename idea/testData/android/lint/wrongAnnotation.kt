// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintLocalSuppressInspection

import android.annotation.SuppressLint
import android.view.View

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class WrongAnnotation2 {
    @SuppressLint("NewApi")
    private val field1: Int = 0

    @SuppressLint("NewApi")
    private val field2 = 5

    companion object {
        @SuppressLint("NewApi") // Valid: class-file check on method
        fun foobar(view: View, @SuppressLint("NewApi") foo: Int) {
            // Invalid: class-file check
            <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@SuppressLint("NewApi")</error> // Invalid
            val a: Boolean

            @SuppressLint("SdCardPath", "NewApi") // TODO: Invalid, class-file based check on local variable
            val b: Boolean

            @android.annotation.SuppressLint("SdCardPath", "NewApi") // TDOD: Invalid (FQN)
            val c: Boolean

            @SuppressLint("SdCardPath") // Valid: AST-based check
            val d: Boolean
        }

        init {
            // Local variable outside method: invalid
            <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@SuppressLint("NewApi")</error>
            val localvar = 5
        }

        private fun test() {
            <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@SuppressLint("NewApi")</error> // Invalid
            val a = View.MEASURED_STATE_MASK
        }
    }
}
