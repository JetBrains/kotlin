// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintSdCardPathInspection

import java.io.File
import android.content.Intent
import android.net.Uri

/**
 * Ignore comments - create("/sdcard/foo")
 */
@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class SdCardTest {
    internal var deviceDir = File("<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard/vr</warning>")

    init {
        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing("<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard/launcher</warning></warning>")
        }

        if (File("<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard</warning></warning>").exists()) {
    }
    val FilePath = "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard/</warning></warning>" + File("test")
    System.setProperty("foo.bar", "file://sdcard")


    val intent = Intent(Intent.ACTION_PICK)
    intent.setDataAndType(Uri.parse("<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">file://sdcard/foo.json</warning></warning>"), "application/bar-json")
    intent.putExtra("path-filter", "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard(/.+)*</warning></warning>")
    intent.putExtra("start-dir", "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard</warning></warning>")
    val mypath = "<warning descr="Do not hardcode \"`/data/`\"; use `Context.getFilesDir().getPath()` instead"><warning descr="Do not hardcode \"`/data/`\"; use `Context.getFilesDir().getPath()` instead">/data/data/foo</warning></warning>"
    val base = "<warning descr="Do not hardcode \"`/data/`\"; use `Context.getFilesDir().getPath()` instead"><warning descr="Do not hardcode \"`/data/`\"; use `Context.getFilesDir().getPath()` instead">/data/data/foo.bar/test-profiling</warning></warning>"
    val s = "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead"><warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">file://sdcard/foo</warning></warning>"

    val sdCardPath by lazy { "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard</warning>" }
    fun localPropertyTest() {
        val sdCardPathLocal by lazy { "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard</warning>" }
    }
}

companion object {
    private val PROFILE_STARTUP = true
    private val SDCARD_TEST_HTML = "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard/test.html</warning>"
    val SDCARD_ROOT = "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard</warning>"
    val PACKAGES_PATH = "<warning descr="Do not hardcode \"/sdcard/\"; use `Environment.getExternalStorageDirectory().getPath()` instead">/sdcard/o/packages/</warning>"
}
}
