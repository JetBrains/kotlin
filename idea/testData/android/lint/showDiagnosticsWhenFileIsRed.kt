// INSPECTION_CLASS: com.android.tools.idea.lint.AndroidLintSQLiteStringInspection

import android.database.sqlite.SQLiteDatabase

fun test(db: SQLiteDatabase) {
    val <warning descr="[UNUSED_VARIABLE] Variable 'a' is never used">a</warning>: String = <error descr="[CONSTANT_EXPECTED_TYPE_MISMATCH] The integer literal does not conform to the expected type String">1</error>

    <warning descr="Using column type STRING; did you mean to use TEXT? (STRING is a numeric type and its value can be adjusted; for example, strings that look like integers can drop leading zeroes. See issue explanation for details.)">db.execSQL("CREATE TABLE COMPANY(NAME STRING)")</warning>
}