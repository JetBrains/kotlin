// "Make prop internal" "true"
// ERROR: Cannot access 'prop': it is 'private' in 'privateTopLevelVarInFile.before.Dependency.kt'

package test

fun foo() {
    <caret>prop = 20
}
