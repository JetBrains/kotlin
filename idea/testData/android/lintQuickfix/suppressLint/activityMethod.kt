// INTENTION_TEXT: Suppress: Add @SuppressLint("SdCardPath") annotation
// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintSdCardPathInspection

import android.app.Activity
import android.os.Environment


class MainActivity : Activity() {
    fun getSdCard(fromEnvironment: Boolean) = if (fromEnvironment) Environment.getExternalStorageDirectory().path else "<caret>/sdcard"
}