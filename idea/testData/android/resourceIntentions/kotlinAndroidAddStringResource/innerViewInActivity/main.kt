package com.myapp

import android.app.Activity
import android.content.Context
import android.view.View

class MyActivity: Activity() {
    inner class SubView constructor(context: Context): View(context) {
        fun test() {
            val b = "some <caret>string"
        }
    }
}