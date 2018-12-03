// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintRecycleInspection

@file:Suppress("UNUSED_VARIABLE")

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cursor = contentResolver.<warning descr="This `Cursor` should be freed up after use with `#close()`">query</warning>(null, null, null, null, null)

        // WARNING
        contentResolver.query(null, null, null, null, null)

        // OK, closed in chained call
        contentResolver.query(null, null, null, null, null).close()

        // KT-14677: Kotlin Lint: "Missing recycle() calls" report cursor with `use()` call
        val cursorUsed = contentResolver.query(null, null, null, null, null)
        cursorUsed.use {  }

        // OK, used in chained call
        contentResolver.query(null, null, null, null, null).use {

        }

        // KT-13372: Android Lint for Kotlin: false positive "Cursor should be freed" inside 'if' expression
        if (true) {
            val c = contentResolver.query(null, null, null, null, null)
            c.close()
        }
    }
}
