// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintSQLiteStringInspection

import android.database.sqlite.SQLiteDatabase

fun test(db: SQLiteDatabase) {
    db.<warning descr="Using column type STRING; did you mean to use TEXT? (STRING is a numeric type and its value can be adjusted; for example, strings that look like integers can drop leading zeroes. See issue explanation for details.)">execSQL("CREATE TABLE COMPANY(NAME STRING)")</warning>
}