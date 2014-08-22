package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*

class MyActivity(): Activity() {
    val textViewWidget = TextView()
    val editTextWidget = EditText()
    val buttonWidget = Button()

    override fun findViewById(id: Int): View? {
        return when (id) {
            R.id.textView1 -> textViewWidget
            R.id.password -> editTextWidget
            R.id.login -> buttonWidget
            else -> null
        }
    }

    fun test(): String {
        return if (password.toString() == "EditText" &&
                   textView1.toString() == "TextView" &&
                   login.toString() == "Button" )
            "OK" else "NOTOK"
    }
}