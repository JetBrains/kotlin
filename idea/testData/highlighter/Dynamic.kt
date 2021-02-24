// IGNORE_FIR
package testing

fun <info textAttributesKey="KOTLIN_FUNCTION_DECLARATION">tst</info>(<info textAttributesKey="KOTLIN_PARAMETER">d</info>: <error><info>dynamic</info></error>) {
    <info textAttributesKey="KOTLIN_PARAMETER">d</info>.<info textAttributesKey="KOTLIN_DYNAMIC_FUNCTION_CALL">foo</info>()
    <info textAttributesKey="KOTLIN_PARAMETER">d</info>.<info textAttributesKey="KOTLIN_DYNAMIC_PROPERTY_CALL">foo</info>
    <info textAttributesKey="KOTLIN_PARAMETER">d</info>.<info textAttributesKey="KOTLIN_DYNAMIC_PROPERTY_CALL">foo</info> = 1
}