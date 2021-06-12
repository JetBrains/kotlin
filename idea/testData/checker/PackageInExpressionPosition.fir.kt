package foo

class X {}

val s = <error descr="[EXPRESSION_EXPECTED_PACKAGE_FOUND] Expression expected, but a package name found">java</error>
val ss = <error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>
val sss = <error descr="[NO_COMPANION_OBJECT] Classifier 'foo/X' does not have a companion object, and thus must be initialized here">X</error>
val x = "${<error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>}"
val xs = java.<error descr="[EXPRESSION_EXPECTED_PACKAGE_FOUND] Expression expected, but a package name found">lang</error>
val xss = java.lang.<error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>
val xsss = foo.<error descr="[NO_COMPANION_OBJECT] Classifier 'foo/X' does not have a companion object, and thus must be initialized here">X</error>
val xssss = <error descr="[EXPRESSION_EXPECTED_PACKAGE_FOUND] Expression expected, but a package name found">foo</error>
val f = { <error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error> }

fun main() {
    <error descr="[EXPRESSION_EXPECTED_PACKAGE_FOUND] Expression expected, but a package name found">java</error> = null
    <error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error> = null
    <error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>!!
    java.lang.<error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error> = null
    java.lang.<error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>!!
    <error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error> is Int
    <error descr="[INVISIBLE_REFERENCE] Symbol java/lang/System.System is invisible">System</error>()
    (<error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>)
    foo@ <error descr="[NO_COMPANION_OBJECT] Classifier 'java/lang/System' does not have a companion object, and thus must be initialized here">System</error>
    null <error descr="[UNRESOLVED_REFERENCE] Unresolved reference: contains">in</error> System
}
