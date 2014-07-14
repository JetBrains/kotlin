import android.app.Activity
import android.view.View
import android.widget.*

val Activity.item_detail_container: FrameLayout
    get() = findViewById(R.id.item_detail_container) as FrameLayout

val Activity.textView1: TextView
    get() = findViewById(R.id.textView1) as TextView

val Activity.password: EditText
    get() = findViewById(R.id.password) as EditText

val Activity.login: Button
    get() = findViewById(R.id.login) as Button

