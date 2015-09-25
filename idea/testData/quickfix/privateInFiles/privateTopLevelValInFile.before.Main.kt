// "Make prop internal" "true"
// ERROR: Cannot access 'prop': it is 'private' in 'privateTopLevelValInFile.before.Dependency.kt'

package test

fun foo() {
    val x = <caret>prop
}
