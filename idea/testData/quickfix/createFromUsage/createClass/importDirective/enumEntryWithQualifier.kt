// "Create enum constant 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create interface 'A'
// ACTION: Create object 'A'
// ACTION: Create enum 'A'
// ACTION: Create annotation 'A'
// ACTION: Rename reference
// ERROR: Unresolved reference: A
package p

import p.X.<caret>A

class X {

}