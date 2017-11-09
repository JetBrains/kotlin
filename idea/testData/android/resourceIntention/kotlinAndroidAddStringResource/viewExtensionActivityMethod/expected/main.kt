package com.myapp

import android.app.Activity
import android.view.View

class MyActivity: Activity() {
    fun View.foo() {
        val a = context.getString(R.string.resource_id)
    }
}