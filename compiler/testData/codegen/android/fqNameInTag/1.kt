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
    val buttonWidget = Button(this)

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.login -> buttonWidget
            else -> null
        }
    }
}

fun box(): String {
    return "OK"
}
