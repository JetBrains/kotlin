package foo

<!ACTUAL_WITHOUT_EXPECT("Class 'A'", " The following declaration is incompatible because visibility is different:     public final expect class A ")!>private<!> class <!PACKAGE_OR_CLASSIFIER_REDECLARATION("A")!>A<!>