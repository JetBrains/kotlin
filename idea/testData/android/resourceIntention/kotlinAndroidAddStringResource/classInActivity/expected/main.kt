package com.myapp

import android.app.Activity
import android.content.Context

class MyActivity: Activity() {
    class Helper {
        fun test(context: Context) {
            val b = context.getString(R.string.resource_id)
        }
    }
}