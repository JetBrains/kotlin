// INTENTION_TEXT: Convert cast to findViewById with type parameter
// INSPECTION_CLASS: org.jetbrains.kotlin.android.inspection.TypeParameterFindViewByIdInspection

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView


class OtherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other)

        val tvHello = <caret>findViewById(R.id.tvHello) as TextView?
    }
}

class R {
    object layout {
        val activity_other = 100500
    }

    object id {
        val tvHello = 0
    }
}