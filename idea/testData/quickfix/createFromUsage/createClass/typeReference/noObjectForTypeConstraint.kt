// "Create object 'NotExistent'" "false"
// ACTION: Create class 'NotExistent'
// ACTION: Create interface 'NotExistent'
// ACTION: Create type alias 'NotExistent'
// ACTION: Create test
// ERROR: Unresolved reference: NotExistent
class TPB<X> where X : <caret>NotExistent