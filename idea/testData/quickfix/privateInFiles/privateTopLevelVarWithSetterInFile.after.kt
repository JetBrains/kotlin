// "Make <set-prop> internal" "true"
// ERROR: Cannot access '<set-prop>': it is 'private' in 'privateTopLevelVarWithSetterInFile.before.Dependency.kt'

package test

fun foo() {
    prop = 20
}
