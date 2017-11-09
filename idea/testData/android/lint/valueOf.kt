// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintUseValueOfInspection

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class Simple {
    fun test() {
        <warning descr="Use `Integer.valueOf(5)` instead">Integer(5)</warning>
    }
}