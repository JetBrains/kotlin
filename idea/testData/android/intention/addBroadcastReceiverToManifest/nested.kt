// INTENTION_CLASS: org.jetbrains.kotlin.android.intention.AddBroadcastReceiverToManifest
// CHECK_MANIFEST
package com.myapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class Test {
    class <caret>MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
        }
    }
}