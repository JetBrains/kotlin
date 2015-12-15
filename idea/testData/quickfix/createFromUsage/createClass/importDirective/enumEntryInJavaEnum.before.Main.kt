// "Create enum constant 'A'" "false"
// ACTION: Create annotation 'A'
// ACTION: Create class 'A'
// ACTION: Create enum 'A'
// ACTION: Create interface 'A'
// ACTION: Rename reference
// ERROR: Unresolved reference: A
import E.<caret>A

class X {

}