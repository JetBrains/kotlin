package com.myapp

import android.app.Activity
import android.content.Context

class MyActivity: Activity() {
    class Helper {
        fun test(context: Context) {
            val b = "some <caret>string"
        }
    }
}