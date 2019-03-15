// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintCommitPrefEditsInspection

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class SharedPrefsText(context: Context) : Activity() {
    // OK 1
    fun onCreate1(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
        editor.commit()
    }

    // OK 2
    fun onCreate2(savedInstanceState: Bundle, apply: Boolean) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = preferences.edit()
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
        if (apply) {
            editor.apply()
        }
    }

    // Not a bug
    fun test(foo: Foo) {
        val bar1 = foo.edit()
        val bar3 = edit()
        apply()
    }

    internal fun apply() {

    }

    fun edit(): Bar {
        return Bar()
    }

    class Foo {
        internal fun edit(): Bar {
            return Bar()
        }
    }

    class Bar

    // Bug
    fun bug1(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = <warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">preferences.edit()</warning>
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
    }

    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = <warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">preferences.edit()</warning>
        editor.putString("foo", "bar")
    }

    fun testResultOfCommit() {
        val r1 = PreferenceManager.getDefaultSharedPreferences(this).edit().putString("wat", "wat").commit()
        val r2 = <warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">PreferenceManager.getDefaultSharedPreferences(this).edit()</warning>.putString("wat", "wat").commit().toString()
    }
}