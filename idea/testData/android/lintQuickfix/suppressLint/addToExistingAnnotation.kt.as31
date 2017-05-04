// INTENTION_TEXT: Suppress: Add @SuppressLint("SdCardPath") annotation
// INSPECTION_CLASS: org.jetbrains.android.inspections.lint.AndroidLintInspectionToolProvider$AndroidLintSdCardPathInspection

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Environment


class MainActivity : Activity() {
    @SuppressLint("Something")
    fun getSdCard(fromEnvironment: Boolean) = if (fromEnvironment) Environment.getExternalStorageDirectory().path else "<caret>/sdcard"
}