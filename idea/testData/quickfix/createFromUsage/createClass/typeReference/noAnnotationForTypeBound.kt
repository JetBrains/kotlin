// "Create annotation 'NotExistent'" "false"
// ACTION: Create class 'NotExistent'
// ACTION: Create interface 'NotExistent'
// ACTION: Create type alias 'NotExistent'
// ACTION: Create type parameter 'NotExistent' in class 'TPB'
// ACTION: Create test
// ERROR: Unresolved reference: NotExistent
class TPB<X : <caret>NotExistent>