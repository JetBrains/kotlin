package com.myapp

import android.app.Activity
import android.content.Context

class MyActivity: Activity() {
    object A {
        fun doSomething(context: Context) {
            context.getString(R.string.resource_id)
        }
    }
}