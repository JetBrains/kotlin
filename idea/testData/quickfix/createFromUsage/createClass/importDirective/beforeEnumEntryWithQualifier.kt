// "Create enum constant 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create trait 'A'
// ACTION: Create object 'A'
// ACTION: Create enum 'A'
// ACTION: Create annotation 'A'
// ERROR: Unresolved reference: A
package p

import p.X.<caret>A

class X {

}