package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*

val Activity.item_detail_container: FrameLayout
    get() = findViewById(0) as FrameLayout

val Activity.textView1: TextView
    get() = findViewById(0) as TextView

val Activity.password: EditText
    get() = findViewById(0) as EditText

val Activity.login: Button
    get() = findViewById(0) as Button

