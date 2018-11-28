// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintSuspiciousImportInspection

//Warning
<warning descr="Don't include `android.R` here; use a fully qualified name for each usage instead">import android.R</warning>

fun a() {
    R.id.button1
}