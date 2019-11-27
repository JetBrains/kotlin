class A {
    val foo = ""

    val bar = A::foo
}

// TESTED_OBJECT_KIND: class
// TESTED_OBJECTS: A$bar$1
// FLAGS: ACC_FINAL, ACC_SUPER, ACC_SYNTHETIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A$bar$1, getName
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A$bar$1, getOwner
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A$bar$1, getSignature
// FLAGS: ACC_PUBLIC

// TESTED_OBJECT_KIND: function
// TESTED_OBJECTS: A$bar$1, get
// FLAGS: ACC_PUBLIC