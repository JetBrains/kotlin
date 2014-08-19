package com.myapp

trait Activity
trait Button


class MyActivity: Activity {
    val button = this.log<caret>
}

// EXIST: login
