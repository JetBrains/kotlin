// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintCommitPrefEditsInspection

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

    // OK using with lambda
    fun withLambda() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        with(preferences.edit()) {
            putString("foo", "bar")
            putInt("bar", 42)
            apply()
        }
    }

    // OK using apply lambda
    fun testApplyLambda() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().apply {
            putString("foo", "bar")
            putInt("bar", 42)
            apply()
        }
    }

    // OK using also lambda
    fun testAlsoLambda() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.edit().also {
            it.putString("foo", "bar")
            it.putInt("bar", 42)
            it.apply()
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
        val editor = preferences.<warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">edit()</warning>
        editor.putString("foo", "bar")
        editor.putInt("bar", 42)
    }

    // Bug missing commit in apply lambda
    fun applyLambdaMissingCommit() {
        PreferenceManager.getDefaultSharedPreferences(this).<warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">edit()</warning>.apply {
            putString("foo", "bar")
            putInt("bar", 42)
        }
    }

    // Bug missing commit in also lambda
    fun alsoLambdaMissingCommit() {
        PreferenceManager.getDefaultSharedPreferences(this).<warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">edit()</warning>.also {
            it.putString("foo", "bar")
            it.putInt("bar", 42)
        }
    }

    // Bug missing commit in with lambda
    fun withLambdaMissingCommit() {
        with(PreferenceManager.getDefaultSharedPreferences(this).<warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">edit()</warning>) {
            putString("foo", "bar")
            putInt("bar", 42)
        }
    }

    init {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = preferences.<warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call"><warning descr="`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call">edit()</warning></warning>
        editor.putString("foo", "bar")
    }

    fun testResultOfCommit() {
        val r1 = PreferenceManager.getDefaultSharedPreferences(this).edit().putString("wat", "wat").commit()
        val r2 = PreferenceManager.getDefaultSharedPreferences(this).edit().putString("wat", "wat").commit().toString()
    }
}