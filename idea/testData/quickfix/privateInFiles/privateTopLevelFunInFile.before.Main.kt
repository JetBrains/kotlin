// "Make f internal" "true"
// ERROR: Cannot access 'f': it is 'private' in 'privateTopLevelFunInFile.before.Dependency.kt'

package test

fun foo() {
    val x = <caret>f()
}
