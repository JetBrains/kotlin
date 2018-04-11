// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintLocalSuppressInspection

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
        fun foobar(view: View, <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@SuppressLint("NewApi")</error> foo: Int) {
            // Invalid: class-file check
            <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@SuppressLint("NewApi")</error> // Invalid
            val a: Boolean

            <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@SuppressLint("SdCardPath", "NewApi")</error> // TODO: Invalid, class-file based check on local variable
            val b: Boolean

            <error descr="The `@SuppressLint` annotation cannot be used on a local variable with the lint check 'NewApi': move out to the surrounding method">@android.annotation.SuppressLint("SdCardPath", "NewApi")</error> // TDOD: Invalid (FQN)
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
