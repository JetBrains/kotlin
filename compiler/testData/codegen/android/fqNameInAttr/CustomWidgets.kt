package org.my.cool

import android.widget.Button
import android.app.Activity

class MyButton(ctx: Activity): Button(ctx) {
    override fun toString(): String {return "Button"}
}
