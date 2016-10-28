// "Import" "true"
// ERROR: Unresolved reference: R

package com.myapp.activity

import com.myapp.R

fun test() {
    val a = <caret>R.layout.activity_test_kotlin
}