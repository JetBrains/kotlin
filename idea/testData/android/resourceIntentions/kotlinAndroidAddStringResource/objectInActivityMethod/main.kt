package com.myapp

import android.app.Activity

class MyActivity: Activity() {
    fun foo() {
        object {
            val text = "some <caret>string"
        }
    }
}