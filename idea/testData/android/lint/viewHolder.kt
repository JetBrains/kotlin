// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintViewHolderInspection

@file:Suppress("NAME_SHADOWING", "unused", "UNUSED_VALUE", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "UNUSED_VARIABLE")

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView
import java.util.ArrayList

@SuppressWarnings("ConstantConditions", "UnusedDeclaration")
abstract class ViewHolderTest : BaseAdapter() {
    override fun getCount() = 0
    override fun getItem(position: Int) = null
    override fun getItemId(position: Int) = 0L

    class Adapter1 : ViewHolderTest() {
        override fun getView(position: Int, convertView: View, parent: ViewGroup) = null
    }

    class Adapter2 : ViewHolderTest() {
        lateinit var mInflater: LayoutInflater

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            // Should use View Holder pattern here
            convertView = mInflater.<warning descr="Unconditional layout inflation from view adapter: Should use View Holder pattern (use recycled view passed into this method as the second parameter) for smoother scrolling">inflate(R.layout.your_layout, null)</warning>

            val text: TextView = convertView.findViewById(R.id.text)
            text.text = "Position " + position

            return convertView
        }
    }

    class Adapter3 : ViewHolderTest() {
        lateinit var mInflater: LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            // Already using View Holder pattern
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.your_layout, null)
            }

            val text: TextView = convertView!!.findViewById(R.id.text)
            text.text = "Position " + position

            return convertView
        }
    }

    class Adapter4 : ViewHolderTest() {
        lateinit var mInflater: LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            // Already using View Holder pattern
            //noinspection StatementWithEmptyBody
            if (convertView != null) {
            } else {
                convertView = mInflater.inflate(R.layout.your_layout, null)
            }

            val text: TextView = convertView!!.findViewById(R.id.text)
            text.text = "Position " + position

            return convertView
        }
    }

    class Adapter5 : ViewHolderTest() {
        lateinit var mInflater: LayoutInflater

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            // Already using View Holder pattern
            convertView = if (convertView == null) mInflater.inflate(R.layout.your_layout, null) else convertView

            val text: TextView = convertView!!.findViewById(R.id.text)
            text.text = "Position " + position

            return convertView
        }
    }

    class Adapter6 : ViewHolderTest() {
        private val mContext: Context? = null
        private var mLayoutInflator: LayoutInflater? = null
        private lateinit var mLapTimes: ArrayList<Double>

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            if (mLayoutInflator == null)
                mLayoutInflator = mContext!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            var v: View? = convertView
            if (v == null) v = mLayoutInflator!!.inflate(R.layout.your_layout, null)

            val listItemHolder: LinearLayout = v!!.findViewById(R.id.laptimes_list_item_holder)
            listItemHolder.removeAllViews()

            for (i in 1..5) {
                val lapItemView: View  = mLayoutInflator!!.inflate(R.layout.laptime_item, null)
                if (i == 0) {
                    val t: TextView = lapItemView.findViewById(R.id.laptime_text)
                }

                val t2: TextView = lapItemView.findViewById(R.id.laptime_text2)
                if (i < mLapTimes.size - 1 && mLapTimes.size > 1) {
                    var laptime = mLapTimes[i] - mLapTimes[i + 1]
                    if (laptime < 0) laptime = mLapTimes[i]
                }

                listItemHolder.addView(lapItemView)

            }
            return v
        }
    }

    class Adapter7 : ViewHolderTest() {
        lateinit var inflater: LayoutInflater

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var rootView: View? = convertView
            val itemViewType = getItemViewType(position)
            when (itemViewType) {
                0 -> {
                    if (rootView != null)
                        return rootView
                    rootView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
                }
            }
            return rootView!!
        }
    }

    class R {
        object layout {
            val your_layout = 1
            val laptime_item = 2
        }

        object id {
            val laptime_text = 1
            val laptime_text2 = 2
            val laptimes_list_item_holder = 3
            val text = 4
        }
    }
}