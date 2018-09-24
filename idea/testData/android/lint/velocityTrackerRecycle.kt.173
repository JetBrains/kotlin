// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintRecycleInspection

@file:Suppress("UNUSED_VARIABLE")

import android.app.Activity
import android.os.Bundle
import android.view.VelocityTracker

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VelocityTracker.<warning descr="This `VelocityTracker` should be recycled after use with `#recycle()`">obtain</warning>()

        VelocityTracker.obtain().recycle()

        val v1 = VelocityTracker.<warning descr="This `VelocityTracker` should be recycled after use with `#recycle()`">obtain</warning>()

        val v2 = VelocityTracker.obtain()
        v2.recycle()
    }
}
