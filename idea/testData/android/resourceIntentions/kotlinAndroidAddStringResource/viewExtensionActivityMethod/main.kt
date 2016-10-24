package com.myapp

import android.app.Activity
import android.view.View

class MyActivity: Activity() {
    fun View.foo() {
        val a = "some <caret>string"
    }
}