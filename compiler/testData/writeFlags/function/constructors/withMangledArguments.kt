inline class A(val x: Int)

class B(val y: A)

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A, <init>
// FLAGS: ACC_PRIVATE, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: B, <init>
// FLAGS: ACC_PRIVATE