// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintRecycleInspection

@file:Suppress("UNUSED_VARIABLE")

import android.app.Activity
import android.os.Bundle
import android.view.VelocityTracker

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VelocityTracker.obtain()

        VelocityTracker.obtain().recycle()

        val v1 = VelocityTracker.<warning descr="This `VelocityTracker` should be recycled after use with `#recycle()`">obtain</warning>()

        val v2 = VelocityTracker.obtain()
        v2.recycle()
    }
}
