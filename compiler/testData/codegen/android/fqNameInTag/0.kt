package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*
import org.my.cool.MyButton

class R {
    class id {
        class object {
            val login = 5
        }
    }
}

class MyActivity(): Activity() {
    val buttonWidget = MyButton(this)

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.login -> buttonWidget
            else -> null
        }
    }

    public fun box(): String {
        return if (login.toString() == "MyButton") "OK" else ""
    }
}

fun box(): String {
    return MyActivity().box()
}
