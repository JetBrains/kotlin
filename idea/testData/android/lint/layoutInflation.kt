// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintInflateParamsInspection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
abstract class LayoutInflationTest : BaseAdapter() {
    lateinit var mInflater: LayoutInflater

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var view = <warning descr="[VARIABLE_WITH_REDUNDANT_INITIALIZER] Variable 'view' initializer is redundant">convertView</warning>
        <warning descr="[UNUSED_VALUE] The value 'mInflater.inflate(R.layout.your_layout, null)' assigned to 'var view: View defined in LayoutInflationTest.getView' is never used">view =</warning> mInflater.inflate(R.layout.your_layout, null)
        <warning descr="[UNUSED_VALUE] The value 'mInflater.inflate(R.layout.your_layout, null, true)' assigned to 'var view: View defined in LayoutInflationTest.getView' is never used">view =</warning> mInflater.inflate(R.layout.your_layout, null, true)
        view = mInflater.inflate(R.layout.your_layout, parent)
        view = WeirdInflater.inflate(view, null)

        return view
    }

    object WeirdInflater {
        fun inflate(view: View, parent: View?) = view
    }

    object R {
        object layout {
            val your_layout = 1
        }
    }
}

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
abstract class LayoutInflationTest2 : BaseAdapter() {
    lateinit var mInflater: LayoutInflater

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View? {
        return if (true) {
            mInflater.inflate(R.layout.your_layout, parent)
        } else {
            null
        }
    }

    object R {
        object layout {
            val your_layout = 1
        }
    }
}