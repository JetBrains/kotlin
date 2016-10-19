// "class org.jetbrains.kotlin.idea.quickfix.ImportFix" "false"
// ACTION: Create annotation 'SomeTest'
// ACTION: Create class 'SomeTest'
// ACTION: Create interface 'SomeTest'
// ACTION: Create enum 'SomeTest'
// ACTION: Create object 'SomeTest'
// ACTION: Rename reference
// ACTION: Add dependency on module...
// ERROR: Unresolved reference: SomeTest

package testing

import testing.<caret>SomeTest